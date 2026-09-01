package com.ecommerce.mapper;

import com.ecommerce.dto.OrderDto;
import com.ecommerce.dto.OrderItemDto;
import com.ecommerce.model.Order;
import com.ecommerce.model.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "userId", source = "user.id")
    OrderDto toDto(Order order);

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    OrderItemDto toDto(OrderItem orderItem);
}
