package com.ordermgr.dto;

import java.time.LocalDateTime;

public record StockDto(
        Long id,
        Long productId,
        Long quantity,
        Long reservedQuantity,
        Long availableQuantity,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
