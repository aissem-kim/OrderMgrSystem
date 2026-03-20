# OrderService 아키텍처 설계 가이드

## 개요

시니어 아키텍트 관점에서 설계한 주문 서비스로, **동시성 제어**와 **트랜잭션 관리**를 핵심으로 합니다.

---

## 1. 동시성 제어 전략

### 1.1 비관적 락 (PESSIMISTIC_WRITE)

```
사용 시점:
┌─────────────────────────────────────────┐
│ 1. 주문 조회 시                          │
│    orderRepository.findByIdWithLock()    │
│                                          │
│ 2. 재고 조회 시                          │
│    stockRepository.findByProductIdWithLock() │
│                                          │
│ 결과: 다른 트랜잭션의 접근 차단         │
└─────────────────────────────────────────┘
```

### 1.2 동시성 이슈 방지 메커니즘

**시나리오: 동시에 2명이 같은 상품 주문**

```
[시간 흐름]

Transaction A                    Transaction B
─────────────                    ─────────────
①검색: Stock(qty=10)            
②비관적 락 획득 ✓               
③가용재고 = 10                  ①검색 요청
④재고 예약: 5                   ②락 대기 ⏳ (Transaction A가 커밋될 때까지)
⑤저장 ✓                         
⑥커밋 ✓                         
                                ③락 획득 ✓
                                ④가용재고 = 5 (업데이트됨)
                                ⑤재고 예약 요청: 7
                                ⑥OutOfStockException ❌
```

---

## 2. 주문 항목 추가 플로우 (addOrderItem)

### 2.1 상세 프로세스

```
addOrderItem(orderId, productId, quantity)
│
├─ 1️⃣ 주문 조회 (비관적 락)
│  └─ orderRepository.findByIdWithLock(orderId)
│     → 다른 트랜잭션 차단
│
├─ 2️⃣ 상품 조회
│  └─ productRepository.findById(productId)
│     → 상품 기본정보 조회
│
├─ 3️⃣ 재고 조회 (비관적 락) ★ 핵심
│  └─ stockRepository.findByProductIdWithLock(productId)
│     → 동시 접근 원천 차단
│
├─ 4️⃣ 재고 가용성 검증
│  ├─ availableQuantity < requestQuantity?
│  ├─ Yes → OutOfStockException 발생 ❌
│  └─ No → 계속 진행
│
├─ 5️⃣ 재고 예약 (원자성 보장)
│  ├─ stock.reserve(quantity)
│  └─ stockRepository.save(stock)
│
├─ 6️⃣ 주문 항목 생성 및 추가
│  ├─ OrderItem 생성
│  └─ order.addOrderItem(item)
│
└─ 7️⃣ 주문 저장 및 커밋
   └─ orderRepository.save(order)
      → @Transactional 커밋 시점에
        모든 변경사항 DB에 반영
```

### 2.2 트랜잭션 경계

```java
@Transactional  // ← 트랜잭션 시작
public OrderDto addOrderItem(Long orderId, AddOrderItemRequest request) {
    // 트랜잭션 내에서 모든 작업 수행
    // - DB 조회, 수정, 삽입이 원자적으로 처리됨
    // - 예외 발생 시 자동 롤백
    // - 성공 시 커밋
}
// ← 트랜잭션 종료
```

---

## 3. 재고 차감 플로우 (shipOrder)

### 3.1 배송 시 재고 최종 차감

```
shipOrder(orderId)
│
├─ 1️⃣ 주문 조회 (비관적 락)
│
├─ 2️⃣ 각 주문 항목별 처리 (순회)
│  │
│  ├─ 재고 조회 (비관적 락)
│  │
│  ├─ 예약 취소
│  │  └─ stock.cancelReservation(quantity)
│  │
│  ├─ 실제 재고 차감 (판매 확정)
│  │  └─ stock.decreaseQuantity(quantity)
│  │
│  └─ 저장
│     └─ stockRepository.save(stock)
│
├─ 3️⃣ 주문 상태 변경
│  └─ order.ship() → SHIPPED
│
└─ 4️⃣ 커밋
   └─ 트랜잭션 내 모든 변경사항 반영
```

### 3.2 재고 상태 변화

```
주문 추가 (addOrderItem)
┌─────────────────────────┐
│ quantity: 100           │
│ reserved: 0             │
│ available: 100          │
└─────────────────────────┘
            ↓ (수량 10 예약)
┌─────────────────────────┐
│ quantity: 100           │
│ reserved: 10            │
│ available: 90           │ ← 구매 가능한 재고
└─────────────────────────┘
            ↓ (배송 시 차감)
┌─────────────────────────┐
│ quantity: 90            │ ← 차감됨
│ reserved: 0             │ ← 취소됨
│ available: 90           │
└─────────────────────────┘
```

---

## 4. 예외 처리 전략

### 4.1 예외 계층

```
예외 계층 구조
│
├─ 비즈니스 예외 (4xx HTTP)
│  ├─ OutOfStockException (409 Conflict)
│  │  └─ 재고 부족 시 발생
│  │
│  └─ IllegalStateException (409 Conflict)
│     └─ 잘못된 상태 전환
│
├─ 입력 예외 (400 Bad Request)
│  └─ IllegalArgumentException
│     └─ 잘못된 인수 (ID 없음, 음수 등)
│
└─ 시스템 예외 (500 Error)
   └─ Exception
      └─ 예상치 못한 오류
```

### 4.2 OutOfStockException 상세 정보

```json
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

---

## 5. 로깅 전략

### 5.1 로그 레벨별 정보

```
INFO:
- 주문 생성/상태 변경
- 배송/배송완료
- 항목 추가 완료

DEBUG:
- 재고 예약/취소/차감
- 세부 작업 단계

WARN:
- 재고 부족
- 상태 전환 실패

ERROR:
- 예상치 못한 시스템 오류
```

### 5.2 로그 예시

```
INFO: 주문 항목 추가: 주문ID=1, 상품ID=10, 수량=5
DEBUG: 재고 조회 (비관적 락): 상품ID=10
DEBUG: 재고 예약 완료: 상품ID=10, 예약수량=5
INFO: 주문 항목 추가 완료: 주문ID=1, 항목수=1, 총액=50000
```

---

## 6. 성능 고려사항

### 6.1 비관적 락의 영향

| 항목 | 설명 |
|------|------|
| 장점 | - 동시성 문제 완전 차단<br>- 데드락 위험 낮음<br>- 코드 간단 |
| 단점 | - 대기 시간 증가<br>- 처리량(throughput) 감소<br>- 높은 동시 요청 상황에서 병목 |
| 최적화 | - 락 시간 최소화<br>- 배치 작업 분산<br>- 읽기 전용 쿼리는 락 미사용 |

### 6.2 권장 사항

- **고트래픽 상황**: 낙관적 락(Optimistic Lock) 고려
- **높은 동시성**: 재고 시스템 분리 (별도 마이크로서비스)
- **배치 작업**: 캐시 레이어(Redis) 활용

---

## 7. 테스트 시나리오

### 7.1 정상 케이스

```
1. 주문 생성
   ✓ 주문번호 생성, 상태=PENDING

2. 상품 추가 (재고 충분)
   ✓ 재고 예약 성공
   ✓ 주문 항목 추가

3. 주문 확인
   ✓ 상태=CONFIRMED

4. 배송
   ✓ 예약된 재고 차감
   ✓ 상태=SHIPPED

5. 배송완료
   ✓ 상태=DELIVERED
```

### 7.2 예외 케이스

```
1. 재고 부족
   ✗ OutOfStockException 발생
   ✗ 재고 예약 안 함
   ✗ 주문 롤백

2. 동시 주문 (2명 동시에 마지막 재고 구매)
   ✓ 첫번째: 성공
   ✓ 두번째: OutOfStockException
   (비관적 락으로 동시성 제어)

3. 잘못된 상태 전환
   ✗ IllegalStateException
   (예: PENDING → SHIPPED 직접 불가)
```

---

## 8. 브랜치 전략

### 8.1 주요 결정 포인트

```
addOrderItem

│
├─ 주문 존재?
│  ├─ No  → IllegalArgumentException
│  └─ Yes → 상품 확인
│
├─ 상품 존재?
│  ├─ No  → IllegalArgumentException
│  └─ Yes → 재고 확인
│
├─ 재고 존재?
│  ├─ No  → IllegalArgumentException
│  └─ Yes → 가용성 검증
│
└─ 가용재고 >= 요청수량?
   ├─ No  → OutOfStockException ❌
   └─ Yes → 예약 및 추가 ✓
```

---

## 9. 마이그레이션 가이드

### 9.1 기존 시스템에서의 변경

```
Before (StockService 사용)
├─ OrderService → StockService → StockRepository
└─ 간접 호출으로 락 제어 불명확

After (직접 비관적 락 사용)
├─ OrderService → StockRepository (with Lock)
└─ 명확한 동시성 제어
```

### 9.2 마이그레이션 체크리스트

- [ ] OutOfStockException 추가
- [ ] StockRepository에 findByProductIdWithLock() 메서드 추가
- [ ] OrderServiceImpl 교체
- [ ] GlobalExceptionHandler 업데이트
- [ ] 전체 integrati테스트 수행
- [ ] 동시성 테스트 (스트래스 테스트) 수행

---

## 10. 참고: SQL 레벨의 락 메커니즘

```sql
-- PostgreSQL 비관적 락 (PESSIMISTIC_WRITE)
SELECT * FROM stocks WHERE product_id = 10 FOR UPDATE;

-- 효과:
-- 1. 행 레벨 락 획득
-- 2. 다른 트랜잭션이 같은 행 접근 차단
-- 3. 첫 번째 트랜잭션 커밋/롤백 시 락 해제
```

---

## 결론

이 설계는 **일관성(Consistency)**과 **동시성(Concurrency)**을 모두 보장하는 
엔터프라이즈 수준의 주문 시스템을 제공합니다.

- ✅ 재고 부족 방지
- ✅ 동시 주문 안전 처리
- ✅ 명확한 예외 처리
- ✅ 트랜잭션 무결성
