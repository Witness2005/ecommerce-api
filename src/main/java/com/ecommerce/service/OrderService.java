package com.ecommerce.service;

import com.ecommerce.dto.OrderDto;
import com.ecommerce.exception.EntityNotFoundException;
import com.ecommerce.mapper.OrderMapper;
import com.ecommerce.model.Cart;
import com.ecommerce.model.CartItem;
import com.ecommerce.model.Order;
import com.ecommerce.model.OrderItem;
import com.ecommerce.model.OrderStatus;
import com.ecommerce.model.User;
import com.ecommerce.repository.CartRepository;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final OrderMapper orderMapper;

    public OrderDto createOrderFromCart(UUID userId, String shippingAddress) {
        User user = findUserById(userId);
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Cart not found for user: " + userId));

        if (cart.getCartItems().isEmpty()) {
            throw new IllegalArgumentException("Cannot create an order from an empty cart");
        }

        Order order = Order.builder()
                .user(user)
                .status(OrderStatus.PENDING)
                .totalPrice(cart.getTotalPrice())
                .shippingAddress(shippingAddress)
                .build();

        for (CartItem cartItem : cart.getCartItems()) {
            order.getOrderItems().add(
                    OrderItem.builder()
                            .order(order)
                            .product(cartItem.getProduct())
                            .quantity(cartItem.getQuantity())
                            .price(cartItem.getPrice())
                            .build()
            );
        }

        Order savedOrder = orderRepository.save(order);

        cart.getCartItems().clear();
        cart.setTotalPrice(BigDecimal.ZERO);
        cartRepository.save(cart);

        log.info("Order {} created from cart for user {}", savedOrder.getId(), userId);

        return orderMapper.toDto(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderDto getOrder(UUID userId, UUID orderId) {
        return orderMapper.toDto(findOwnedOrder(userId, orderId));
    }

    @Transactional(readOnly = true)
    public List<OrderDto> getUserOrders(UUID userId) {
        findUserById(userId);
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(orderMapper::toDto)
                .toList();
    }

    public OrderDto updateOrderStatus(UUID orderId, OrderStatus status) {
        Order order = findOrderById(orderId);
        order.setStatus(status);
        order = orderRepository.save(order);
        log.info("Order {} status updated to {}", orderId, status);

        return orderMapper.toDto(order);
    }

    public OrderDto cancelOrder(UUID userId, UUID orderId) {
        Order order = findOwnedOrder(userId, orderId);

        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Order cannot be cancelled in status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        order = orderRepository.save(order);
        log.info("Order {} cancelled for user {}", orderId, userId);

        return orderMapper.toDto(order);
    }

    private Order findOrderById(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));
    }

    private Order findOwnedOrder(UUID userId, UUID orderId) {
        Order order = findOrderById(orderId);
        if (!order.getUser().getId().equals(userId)) {
            throw new EntityNotFoundException("Order not found: " + orderId);
        }
        return order;
    }

    private User findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
    }
}
