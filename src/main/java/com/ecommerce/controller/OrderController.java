package com.ecommerce.controller;

import com.ecommerce.dto.CreateOrderRequest;
import com.ecommerce.dto.OrderDto;
import com.ecommerce.dto.OrderStatusDto;
import com.ecommerce.model.User;
import com.ecommerce.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("isAuthenticated()")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderDto> createOrder(@AuthenticationPrincipal User user,
                                                 @Valid @RequestBody CreateOrderRequest request) {
        OrderDto order = orderService.createOrderFromCart(user.getId(), request.getShippingAddress());
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @GetMapping
    public ResponseEntity<List<OrderDto>> getUserOrders(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(orderService.getUserOrders(user.getId()));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDto> getOrder(@AuthenticationPrincipal User user,
                                              @PathVariable UUID orderId) {
        return ResponseEntity.ok(orderService.getOrder(user.getId(), orderId));
    }

    @PutMapping("/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderDto> updateOrderStatus(@PathVariable UUID orderId,
                                                        @Valid @RequestBody OrderStatusDto request) {
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, request.getStatus()));
    }

    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<OrderDto> cancelOrder(@AuthenticationPrincipal User user,
                                                 @PathVariable UUID orderId) {
        return ResponseEntity.ok(orderService.cancelOrder(user.getId(), orderId));
    }
}
