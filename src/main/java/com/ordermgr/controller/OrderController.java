package com.ordermgr.controller;

import com.ordermgr.dto.AddOrderItemRequest;
import com.ordermgr.dto.OrderCreateRequest;
import com.ordermgr.dto.OrderDto;
import com.ordermgr.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderDto> createOrder(@RequestBody OrderCreateRequest request) {
        OrderDto order = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDto> getOrder(@PathVariable Long orderId) {
        OrderDto order = orderService.getOrder(orderId);
        return ResponseEntity.ok(order);
    }

    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<OrderDto> getOrderByOrderNumber(@PathVariable String orderNumber) {
        OrderDto order = orderService.getOrderByOrderNumber(orderNumber);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/{orderId}/items")
    public ResponseEntity<OrderDto> addOrderItem(
            @PathVariable Long orderId,
            @RequestBody AddOrderItemRequest request) {
        OrderDto order = orderService.addOrderItem(orderId, request);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<OrderDto> confirmOrder(@PathVariable Long orderId) {
        OrderDto order = orderService.confirmOrder(orderId);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderDto> cancelOrder(@PathVariable Long orderId) {
        OrderDto order = orderService.cancelOrder(orderId);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/{orderId}/ship")
    public ResponseEntity<OrderDto> shipOrder(@PathVariable Long orderId) {
        OrderDto order = orderService.shipOrder(orderId);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/{orderId}/deliver")
    public ResponseEntity<OrderDto> deliverOrder(@PathVariable Long orderId) {
        OrderDto order = orderService.deliverOrder(orderId);
        return ResponseEntity.ok(order);
    }
}
