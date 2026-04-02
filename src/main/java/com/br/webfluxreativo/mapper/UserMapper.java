package com.br.webfluxreativo.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.br.webfluxreativo.entity.UserEntity;
import com.br.webfluxreativo.model.dtos.UserRequest;
import com.br.webfluxreativo.model.dtos.UserResponse;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserEntity toEntity(UserRequest request);

    UserResponse toResponse(UserEntity entity);

    // update existing entity from request
    @Mapping(target = "id", ignore = true)
    void updateFromRequest(UserRequest request, @MappingTarget UserEntity entity);

}

