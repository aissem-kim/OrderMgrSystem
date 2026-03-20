package com.ordermgr.repository;

import com.ordermgr.domain.entity.Stock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {

    /**
     * 비관적 락(PESSIMISTIC_WRITE)을 사용한 재고 조회
     * 동시성 이슈 방지: 다른 트랜잭션의 접근을 차단하여 동시 수정 방지
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Stock s WHERE s.id = :id")
    Optional<Stock> findByIdWithLock(@Param("id") Long id);

    /**
     * 상품 ID로 비관적 락과 함께 재고 조회
     * 주문 추가 시 재고 확인 및 차감을 원자적으로 처리
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Stock s WHERE s.product.id = :productId")
    Optional<Stock> findByProductIdWithLock(@Param("productId") Long productId);

    /**
     * 일반 조회 (읽기 전용)
     */
    Optional<Stock> findByProductId(Long productId);
}
