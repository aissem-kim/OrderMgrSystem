- **Environment**: Java 21, Spring Boot 3.3+, Maven, PostgreSQL.
- **Architecture**: Domain-Driven Design (DDD) 기반 레이어드 아키텍처.
- **Rules**:
  1. DTO는 Java `record` 타입을 사용한다.
  2. 모든 비즈니스 로직은 인터페이스 기반의 Service 레이어에 구현하며 `@Transactional`을 필수 적용한다.
  3. 동시성 제어를 위해 JPA `@Lock(LockModeType.PESSIMISTIC_WRITE)`를 사용한다.
  4. 예외 처리는 `@RestControllerAdvice`를 통해 공통 형식으로 응답한다.

