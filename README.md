# Order Management System

Java 21, Spring Boot 3.3, JPA, PostgreSQL을 사용하는 주문 관리 시스템입니다.

## 🎯 시니어 아키텍트 설계 특징

### 1. 동시성 제어 (PESSIMISTIC_WRITE)
- **비관적 락**으로 동시 주문 시 재고 이중 차감 방지
- 트랜잭션 내 모든 조회 및 수정이 원자적으로 처리
- 높은 일관성 보장

### 2. 명확한 예외 처리
- `OutOfStockException`: 재고 부족 시 명시적 예외
- `OrderException`: 주문 관련 기본 예외
- 예외별 HTTP 상태 코드 매핑 (409 Conflict 등)

### 3. 트랜잭션 관리
- 모든 비즈니스 로직에 `@Transactional` 필수 적용
- 예외 발생 시 자동 롤백
- 읽기 전용 메서드는 `readOnly=true`로 최적화

### 4. 로깅 및 모니터링
- `@Slf4j`를 활용한 구조화된 로깅
- 주요 비즈니스 이벤트 추적
- 동시성 문제 디버깅 가능

---

## 📁 프로젝트 구조

```
src/main/java/com/ordermgr
├── OrderMgrApplication.java       # 메인 애플리케이션
├── controller/                     # REST 컨트롤러
│   ├── ProductController.java
│   ├── StockController.java
│   └── OrderController.java
├── service/                        # 비즈니스 로직 (⭐ 개선됨)
│   ├── ProductService.java / ProductServiceImpl.java
│   ├── StockService.java / StockServiceImpl.java
│   ├── OrderService.java
│   └── OrderServiceImpl.java        # 비관적 락 적용!
├── domain/
│   └── entity/                     # JPA 엔티티
│       ├── Product.java
│       ├── Stock.java              # 재고 관리
│       ├── Order.java              # 상태 머신
│       └── OrderItem.java
├── repository/                     # Data Access Layer
│   ├── ProductRepository.java
│   ├── StockRepository.java        # findByProductIdWithLock() 추가
│   ├── OrderRepository.java
│   └── OrderItemRepository.java
├── dto/                            # Data Transfer Objects (record)
│   └── [DTO 클래스들]
└── exception/                      # 예외 처리
    ├── OutOfStockException.java    # ⭐ 비즈니스 예외
    ├── OrderException.java
    ├── ApiResponse.java
    └── GlobalExceptionHandler.java # ⭐ 예외 핸들러 확장
```

---

## 🔒 동시성 제어 메커니즘

### 핵심: PESSIMISTIC_WRITE 락

```java
// StockRepository
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT s FROM Stock s WHERE s.product.id = :productId")
Optional<Stock> findByProductIdWithLock(@Param("productId") Long productId);

// OrderServiceImpl - addOrderItem()
@Transactional
public OrderDto addOrderItem(Long orderId, AddOrderItemRequest request) {
    // 1. 주문 조회 (락)
    Order order = orderRepository.findByIdWithLock(orderId);
    
    // 2. 재고 조회 (락) 👈 동시성 제어 핵심
    Stock stock = stockRepository.findByProductIdWithLock(productId);
    
    // 3. 가용성 검증 → OutOfStockException 발생 가능
    if (stock.getAvailableQuantity() < quantity) {
        throw new OutOfStockException(...);
    }
    
    // 4. 재고 예약 및 저장 (원자적)
    stock.reserve(quantity);
    stockRepository.save(stock);
    ...
}
```

### 동시성 시나리오

```
[상황] 재고 10개, 2명 동시에 8개, 5개 구매 시도

Transaction A                          Transaction B
──────────────────                     ──────────────────
Lock: Stock(qty=10)                    Lock 대기 ⏳
Check: 10 >= 8? ✓
Reserve: 8                             Lock 획득 (A 커밋 後)
Commit ✓                               Check: 10 >= 5? ❌
                                       OutOfStockException ❌

결과: A만 성공, B는 재고 부족
```

---

## 💼 비즈니스 로직 흐름

### 필드1: 주문 항목 추가 (addOrderItem)

1. **주문 조회** (비관적 락)
   - 다른 트랜잭션의 수정 차단

2. **상품 확인**
   - 상품 존재 여부 검범

3. **재고 조회** (비관적 락) ⭐
   - 재고에 대한 동시 접근 원천 차단
   - 다른 트랜잭션이 커밋될 때까지 대기

4. **가용성 검증**
   - 가용재고 < 요청수량 → `OutOfStockException` 발생
   - 구매 거부 & 롤백

5. **재고 예약**
   - 가용재고에서 대기 중인 주문 수량 분리
   - 다른 고객이 구매 불가 상태로 변경

6. **주문 항목 추가**
   - OrderItem 생성 및 주문에 추가
   - 총액 재계산

7. **트랜잭션 커밋**
   - 모든 변경사항을 DB에 원자적으로 반영

### 필드2: 배송 (shipOrder)

1. **주문 조회** (비관적 락)
2. **각 항목별 재고 처리**
   - 재고 조회 (비관적 락)
   - 예약 취소
   - 실제 재고 차감
3. **주문 상태 변경** (SHIPPED)
4. **트랜잭션 커밋**

### 재고 상태 변화

```
초기 상태: quantity=100, reserved=0, available=100

주문 추가 ↓
        quantity=100, reserved=10, available=90

배송 ↓
        quantity=90, reserved=0, available=90
```

---

## 🚨 예외 처리

### OutOfStockException

```json
HTTP 409 Conflict
{
  "status": 409,
  "message": "재고 부족: 상품 ID=1, 요청 수량=20, 사용 가능 수량=5",
  "data": {
    "productId": 1,
    "requestQuantity": 20,
    "availableQuantity": 5
  },
  "timestamp": "2026-03-20T10:30:00"
}
```

### 예외 계층

| 예외 | HTTP 상태 | 원인 |
|------|----------|------|
| `OutOfStockException` | 409 | 재고 부족 |
| `IllegalStateException` | 409 | 잘못된 상태 전환 |
| `IllegalArgumentException` | 400 | 잘못된 입력 데이터 |
| `Exception` | 500 | 예상치 못한 오류 |

---

## 📊 기술 스택

- **Java**: 21
- **Framework**: Spring Boot 3.3.1
- **Database**: PostgreSQL 16
- **Build Tool**: Maven
- **ORM**: JPA/Hibernate
- **동시성 제어**: PESSIMISTIC_WRITE Lock
- **로깅**: Lombok @Slf4j
- **테스트**: JUnit 5, AssertJ

---

## 🏗️ 엔티티 설계

### Product (상품)
- id, name, description, price
- createdAt, updatedAt

### Stock (재고)
- id, product (1:1), quantity, reservedQuantity
- **version** (낙관적 잠금용)
- **getAvailableQuantity()** = quantity - reservedQuantity

### Order (주문)
- id, orderNumber (유니크), customerName, customerEmail, customerPhone
- **status** (PENDING → CONFIRMED → SHIPPED → DELIVERED / CANCELLED)
- totalAmount, orderItems (1:N)
- createdAt, updatedAt, shippedAt
- **version** (낙관적 잠금용)

### OrderItem (주문 항목)
- id, order (N:1), product (N:1)
- quantity, unitPrice, totalPrice
- createdAt

---

## ⚙️ 환경 구성

### Docker Compose 실행

```bash
docker-compose up -d
```

### 직접 PostgreSQL 접속

```bash
docker exec -it ordermgr-postgres psql -U ordermgr -d ordermgr_db
```

### 설정 파일

**application.yml**
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ordermgr_db
    username: ordermgr
    password: ordermgr123
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
```

---

## 🚀 빌드 및 실행

```bash
# 1. 프로젝트 빌드
mvn clean package

# 2. 애플리케이션 실행
mvn spring-boot:run

# 또는
java -jar target/order-management-system-1.0.0.jar
```

애플리케이션은 `http://localhost:8080/api`로 실행됩니다.

---

## 📡 API 엔드포인트

### 상품 관리
```
POST   /api/products                  - 상품 생성
GET    /api/products/{productId}      - 상품 조회
GET    /api/products                  - 전체 상품 조회
PUT    /api/products/{productId}      - 상품 수정
DELETE /api/products/{productId}      - 상품 삭제
```

### 재고 관리
```
POST   /api/stocks/{productId}?quantity=100                      - 재고 초기화
GET    /api/stocks/{stockId}                                      - 재고 조회
GET    /api/stocks/product/{productId}                            - 상품별 재고 조회
POST   /api/stocks/{stockId}/reserve?quantity=10                  - 재고 예약
POST   /api/stocks/{stockId}/cancel-reservation?quantity=10       - 예약 취소
POST   /api/stocks/{stockId}/decrease?quantity=10                 - 재고 감소
POST   /api/stocks/{stockId}/increase?quantity=10                 - 재고 증가
```

### 주문 관리
```
POST   /api/orders                     - 주문 생성
GET    /api/orders/{orderId}           - 주문 조회
GET    /api/orders/number/{orderNumber} - 주문번호로 조회
POST   /api/orders/{orderId}/items     - 주문 항목 추가
POST   /api/orders/{orderId}/confirm   - 주문 확인
POST   /api/orders/{orderId}/cancel    - 주문 취소
POST   /api/orders/{orderId}/ship      - 주문 배송
POST   /api/orders/{orderId}/deliver   - 배송 완료
```

---

## 🧪 테스트

### 통합 테스트 실행

```bash
mvn test

# 또는 특정 테스트 클래스만
mvn test -Dtest=OrderServiceIntegrationTest
```

### 테스트 시나리오

✅ **정상 플로우**: 주문 생성 → 항목 추가 → 확인 → 배송 → 배송완료

✅ **재고 부족**: OutOfStockException 발생 & 주문 롤백

✅ **동시성 제어**: 여러 사용자 동시 주문 안전 처리

✅ **상태 전환**: 잘못된 상태 전환 방지

---

## 📚 아키텍처 문서

자세한 아키텍처 설계, 동시성 제어 메커니즘, 성능 최적화 가이드는 [ARCHITECTURE.md](ARCHITECTURE.md) 참조.

---

## 💡 핵심 설계 원칙

1. **DDD 기반 아키텍처**
   - 명확한 도메인 객체 (엔티티, VO)
   - 서비스 레이어의 비즈니스 로직 집중

2. **PESSIMISTIC_WRITE 락**
   - 높은 동시성 환경에서 데이터 일관성 보장
   - 동시 접근 원천 차단

3. **명시적 예외 처리**
   - 비즈니스 예외 (OutOfStockException)
   - 기술 예외 분리
   - 전역 예외 핸들러 일원화

4. **트랜잭션 무결성**
   - 모든 비즈니스 로직에 @Transactional 적용
   - 원자성(Atomicity) 보장

5. **구조화된 로깅**
   - 주요 비즈니스 이벤트 추적
   - 동시성 문제 디버깅 용이

---

## 🔄 마이그레이션 가이드

기존 시스템에서 이 개선된 OrderService로 마이그레이션하는 경우:

1. [ ] `OutOfStockException` 추가
2. [ ] `StockRepository.findByProductIdWithLock()` 메서드 추가
3. [ ] `OrderServiceImpl` 전체 교체
4. [ ] `GlobalExceptionHandler` 업데이트
5. [ ] 통합 테스트 수행
6. [ ] 동시성 테스트 (스트레스 테스트) 수행
7. [ ] 모니터링 및 로그 검증

---

## 📝 로깅 예시

```
INFO  - 주문 생성: 주문번호=ORD-1710910200000-a1b2c3d4, 고객=김철수
INFO  - 주문 항목 추가: 주문ID=1, 상품ID=10, 수량=5
DEBUG - 재고 조회 (비관적 락): 상품ID=10
DEBUG - 재고 예약 완료: 상품ID=10, 예약수량=5
INFO  - 주문 항목 추가 완료: 주문ID=1, 항목수=1, 총액=50000
INFO  - 주문 배송: 주문ID=1, 상태=SHIPPED, 배송시간=2026-03-20T10:30:00
```

---

## 🎓 참고 자료

- [Spring Data JPA - Lock](https://spring.io/blog/2023/01/23/spring-data-jpa-repositories-documentation)
- [JPA Pessimistic Locking](https://docs.jboss.org/hibernate/orm/6.0/userguide/html_single/Hibernate_User_Guide.html#locking)
- [PostgreSQL Lock Management](https://www.postgresql.org/docs/current/explicit-locking.html)

---

## 📞 지원

문제나 개선 사항이 있으면 이슈를 생성하세요.