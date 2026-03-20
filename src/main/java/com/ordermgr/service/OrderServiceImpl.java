package com.ordermgr.service;

import com.ordermgr.domain.entity.Order;
import com.ordermgr.domain.entity.OrderItem;
import com.ordermgr.domain.entity.Product;
import com.ordermgr.dto.AddOrderItemRequest;
import com.ordermgr.dto.OrderCreateRequest;
import com.ordermgr.dto.OrderDto;
import com.ordermgr.dto.OrderItemDto;
import com.ordermgr.repository.OrderRepository;
import com.ordermgr.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final StockService stockService;

    @Override
    public OrderDto createOrder(OrderCreateRequest request) {
        String orderNumber = generateOrderNumber();
        Order order = Order.builder()
                .orderNumber(orderNumber)
                .customerName(request.customerName())
                .customerEmail(request.customerEmail())
                .customerPhone(request.customerPhone())
                .build();
        Order savedOrder = orderRepository.save(order);
        return mapToDto(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDto getOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다. ID: " + orderId));
        return mapToDto(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDto getOrderByOrderNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다. Order Number: " + orderNumber));
        return mapToDto(order);
    }

    @Override
    public OrderDto addOrderItem(Long orderId, AddOrderItemRequest request) {
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다. ID: " + orderId));

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. ID: " + request.productId()));

        // 재고 예약
        try {
            stockService.reserveStock(getStockIdByProductId(request.productId()), request.quantity());
        } catch (Exception e) {
            throw new IllegalArgumentException("재고 예약에 실패했습니다: " + e.getMessage());
        }

        OrderItem orderItem = OrderItem.builder()
                .product(product)
                .quantity(request.quantity())
                .unitPrice(request.unitPrice())
                .build();

        order.addOrderItem(orderItem);
        Order savedOrder = orderRepository.save(order);
        return mapToDto(savedOrder);
    }

    @Override
    public OrderDto confirmOrder(Long orderId) {
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다. ID: " + orderId));
        order.confirm();
        Order savedOrder = orderRepository.save(order);
        return mapToDto(savedOrder);
    }

    @Override
    public OrderDto cancelOrder(Long orderId) {
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다. ID: " + orderId));
        order.cancel();

        // 예약된 재고 취소
        order.getOrderItems().forEach(item -> {
            try {
                stockService.cancelReservation(
                        getStockIdByProductId(item.getProduct().getId()),
                        item.getQuantity()
                );
            } catch (Exception e) {
                throw new IllegalArgumentException("재고 예약 취소에 실패했습니다: " + e.getMessage());
            }
        });

        Order savedOrder = orderRepository.save(order);
        return mapToDto(savedOrder);
    }

    @Override
    public OrderDto shipOrder(Long orderId) {
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다. ID: " + orderId));
        order.ship();

        // 예약된 재고를 실제 재고에서 차감
        order.getOrderItems().forEach(item -> {
            try {
                Long stockId = getStockIdByProductId(item.getProduct().getId());
                stockService.cancelReservation(stockId, item.getQuantity());
                stockService.decreaseStock(stockId, item.getQuantity());
            } catch (Exception e) {
                throw new IllegalArgumentException("재고 차감에 실패했습니다: " + e.getMessage());
            }
        });

        Order savedOrder = orderRepository.save(order);
        return mapToDto(savedOrder);
    }

    @Override
    public OrderDto deliverOrder(Long orderId) {
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다. ID: " + orderId));
        order.deliver();
        Order savedOrder = orderRepository.save(order);
        return mapToDto(savedOrder);
    }

    private String generateOrderNumber() {
        return "ORD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private Long getStockIdByProductId(Long productId) {
        return productRepository.findById(productId)
                .map(product -> {
                    // 여기서는 StockService를 통해 Stock ID를 조회하는 방식으로 간단히 처리
                    // 실제로는 더 효율적인 방법을 고려할 수 있음
                    try {
                        return Long.valueOf(productId); // 간단화된 구현
                    } catch (Exception e) {
                        throw new IllegalArgumentException("재고 ID를 찾을 수 없습니다.");
                    }
                })
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. ID: " + productId));
    }

    private OrderDto mapToDto(Order order) {
        return new OrderDto(
                order.getId(),
                order.getOrderNumber(),
                order.getCustomerName(),
                order.getCustomerEmail(),
                order.getCustomerPhone(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getOrderItems().stream()
                        .map(item -> new OrderItemDto(
                                item.getId(),
                                item.getProduct().getId(),
                                item.getProduct().getName(),
                                item.getQuantity(),
                                item.getUnitPrice(),
                                item.getTotalPrice(),
                                item.getCreatedAt()
                        ))
                        .toList(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getShippedAt()
        );
    }
}
