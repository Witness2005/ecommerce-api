package com.ecommerce.mapper;

import com.ecommerce.dto.ProductDto;
import com.ecommerce.model.Product;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = CategoryMapper.class)
public interface ProductMapper {

    ProductDto toDto(Product product);
}
