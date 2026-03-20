package com.ordermgr.exception;

/**
 * 주문 관련 비즈니스 예외 - 기본 클래스
 * 
 * 시니어 아키텍트 설계:
 * - 모든 주문 관련 비즈니스 예외의 기본 클래스
 * - 런타임 예외(RuntimeException)로 @Transactional 롤백 자동 트리거
 */
public class OrderException extends RuntimeException {

    public OrderException(String message) {
        super(message);
    }

    public OrderException(String message, Throwable cause) {
        super(message, cause);
    }
}
