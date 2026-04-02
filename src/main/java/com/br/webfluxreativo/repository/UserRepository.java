package com.br.webfluxreativo.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import com.br.webfluxreativo.entity.UserEntity;

public interface UserRepository extends ReactiveMongoRepository<UserEntity, String> {

}

