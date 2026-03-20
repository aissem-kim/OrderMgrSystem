package com.ordermgr.exception;

/**
 * 재고 부족 시 발생하는 비즈니스 예외
 * 
 * 시니어 아키텍트 설계 원칙:
 * - 비즈니스 로직 관련 예외를 명시적으로 정의
 * - 복구 가능한 예외로 클라이언트에 명확한 상태 코드 반환
 * - 동시성 제어 실패 시 명확한 메시지 제공
 */
public class OutOfStockException extends RuntimeException {

    private final Long productId;
    private final Long requestQuantity;
    private final Long availableQuantity;

    public OutOfStockException(Long productId, Long requestQuantity, Long availableQuantity) {
        super(String.format(
                "재고 부족: 상품 ID=%d, 요청 수량=%d, 사용 가능 수량=%d",
                productId, requestQuantity, availableQuantity
        ));
        this.productId = productId;
        this.requestQuantity = requestQuantity;
        this.availableQuantity = availableQuantity;
    }

    public Long getProductId() {
        return productId;
    }

    public Long getRequestQuantity() {
        return requestQuantity;
    }

    public Long getAvailableQuantity() {
        return availableQuantity;
    }
}
