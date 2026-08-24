package com.ecommerce.mapper;

import com.ecommerce.dto.CategoryDto;
import com.ecommerce.model.Category;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    CategoryDto toDto(Category category);

    Category toEntity(CategoryDto dto);
}
