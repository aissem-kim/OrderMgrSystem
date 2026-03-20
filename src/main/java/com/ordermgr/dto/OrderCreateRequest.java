package com.ordermgr.dto;

public record OrderCreateRequest(
        String customerName,
        String customerEmail,
        String customerPhone
) {
}
