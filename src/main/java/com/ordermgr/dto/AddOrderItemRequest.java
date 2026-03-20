package com.ordermgr.dto;

import java.math.BigDecimal;

public record AddOrderItemRequest(
        Long productId,
        Long quantity,
        BigDecimal unitPrice
) {
}
