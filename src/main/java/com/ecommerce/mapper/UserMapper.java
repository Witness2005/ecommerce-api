package com.ecommerce.mapper;

import com.ecommerce.dto.UserDto;
import com.ecommerce.dto.UserProfileDto;
import com.ecommerce.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDto toDto(User user);

    UserProfileDto toProfileDto(User user);
}
