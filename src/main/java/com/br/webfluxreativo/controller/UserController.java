package com.br.webfluxreativo.controller;


import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.br.webfluxreativo.mapper.UserMapper;
import com.br.webfluxreativo.model.dtos.UserRequest;
import com.br.webfluxreativo.model.dtos.UserResponse;
import com.br.webfluxreativo.repository.UserRepository;
import com.br.webfluxreativo.service.UserService;
import java.net.URI;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(value = "/users", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class UserController {

    private final UserService service;
    private final UserMapper mapper; // ainda usado para atualizar entidades locais no controller quando necessário

    // GET /users
    @GetMapping
    public Flux<UserResponse> getAll() {
        return service.findAll();
    }

    // GET /users/{id}
    @GetMapping("/{id}")
    public Mono<ResponseEntity<UserResponse>> getById(@PathVariable String id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    // POST /users
    @PostMapping
    public Mono<ResponseEntity<UserResponse>> create(@Valid @RequestBody Mono<UserRequest> userRequestMono) {
        return userRequestMono
                .flatMap(service::create)
                .map(saved -> ResponseEntity.created(URI.create("/users/" + saved.id()))
                        .body(saved));
    }

    // PUT /users/{id}
    @PutMapping("/{id}")
    public Mono<ResponseEntity<UserResponse>> update(@PathVariable String id, @RequestBody Mono<UserRequest> userRequestMono) {
        return userRequestMono
                .flatMap(req -> service.update(id, req))
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    // DELETE /users/{id}
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable String id) {
        return service.delete(id)
                .flatMap(deleted -> deleted ? Mono.just(ResponseEntity.noContent().<Void>build()) : Mono.just(ResponseEntity.notFound().build()));
    }

}

