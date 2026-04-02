package com.br.webfluxreativo.service;

import com.br.webfluxreativo.mapper.UserMapper;
import com.br.webfluxreativo.model.dtos.UserRequest;
import com.br.webfluxreativo.model.dtos.UserResponse;
import com.br.webfluxreativo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Camada de serviço que encapsula o acesso ao repositório e mapeamentos.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repository;
    private final UserMapper mapper;

    public Flux<UserResponse> findAll() {
        return repository.findAll().map(mapper::toResponse);
    }

    public Mono<UserResponse> findById(String id) {
        return repository.findById(id).map(mapper::toResponse);
    }

    public Mono<UserResponse> create(UserRequest request) {
        return Mono.just(mapper.toEntity(request))
                .flatMap(repository::save)
                .map(mapper::toResponse);
    }

    public Mono<UserResponse> update(String id, UserRequest request) {
        return repository.findById(id)
                .flatMap(existing -> {
                    mapper.updateFromRequest(request, existing);
                    return repository.save(existing);
                })
                .map(mapper::toResponse);
    }

    public Mono<Boolean> delete(String id) {
        return repository.findById(id)
                .flatMap(existing -> repository.delete(existing).thenReturn(true))
                .defaultIfEmpty(false);
    }

}

