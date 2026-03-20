package com.ordermgr.service;

import com.ordermgr.dto.AddOrderItemRequest;
import com.ordermgr.dto.OrderCreateRequest;
import com.ordermgr.dto.OrderDto;

public interface OrderService {
    OrderDto createOrder(OrderCreateRequest request);
    OrderDto getOrder(Long orderId);
    OrderDto getOrderByOrderNumber(String orderNumber);
    OrderDto addOrderItem(Long orderId, AddOrderItemRequest request);
    OrderDto confirmOrder(Long orderId);
    OrderDto cancelOrder(Long orderId);
    OrderDto shipOrder(Long orderId);
    OrderDto deliverOrder(Long orderId);
}
