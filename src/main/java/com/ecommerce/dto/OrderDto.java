package com.ecommerce.dto;

import com.ecommerce.model.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDto {

    private UUID id;

    private UUID userId;

    private List<OrderItemDto> orderItems;

    private OrderStatus status;

    private BigDecimal totalPrice;

    private String shippingAddress;

    private LocalDateTime createdAt;
}
