package com.ordermgr.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "stocks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Product product;

    @Column(name = "quantity", nullable = false)
    private Long quantity;

    @Column(name = "reserved_quantity", nullable = false)
    private Long reservedQuantity = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.reservedQuantity == null) {
            this.reservedQuantity = 0L;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Builder
    public Stock(Product product, Long quantity) {
        this.product = product;
        this.quantity = quantity;
        this.reservedQuantity = 0L;
    }

    /**
     * 재고 증가
     */
    public void increaseQuantity(Long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("증가 수량은 0보다 커야 합니다.");
        }
        this.quantity += amount;
    }

    /**
     * 재고 감소 (구매)
     */
    public void decreaseQuantity(Long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("감소 수량은 0보다 커야 합니다.");
        }
        if (this.quantity - this.reservedQuantity < amount) {
            throw new IllegalArgumentException("재고 부족합니다.");
        }
        this.quantity -= amount;
    }

    /**
     * 재고 예약
     */
    public void reserve(Long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("예약 수량은 0보다 커야 합니다.");
        }
        if (this.quantity - this.reservedQuantity < amount) {
            throw new IllegalArgumentException("예약 가능한 재고가 부족합니다.");
        }
        this.reservedQuantity += amount;
    }

    /**
     * 재고 예약 취소
     */
    public void cancelReservation(Long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("취소 수량은 0보다 커야 합니다.");
        }
        if (this.reservedQuantity < amount) {
            throw new IllegalArgumentException("취소할 예약이 부족합니다.");
        }
        this.reservedQuantity -= amount;
    }

    /**
     * 사용 가능한 재고 조회
     */
    public Long getAvailableQuantity() {
        return this.quantity - this.reservedQuantity;
    }
}
