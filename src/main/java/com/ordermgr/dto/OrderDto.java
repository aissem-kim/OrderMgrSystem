package com.ordermgr.dto;

import com.ordermgr.domain.entity.Order.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderDto(
        Long id,
        String orderNumber,
        String customerName,
        String customerEmail,
        String customerPhone,
        OrderStatus status,
        BigDecimal totalAmount,
        List<OrderItemDto> orderItems,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime shippedAt
) {
}
