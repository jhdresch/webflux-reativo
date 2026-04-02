package com.br.webfluxreativo;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestMongoConfig {

    // Provide a mock ReactiveMongoTemplate so tests that load the full Spring context
    // don't fail when the real Mongo auto-configuration is excluded.
    @Bean
    @Primary
    public ReactiveMongoTemplate reactiveMongoTemplate() {
        return mock(ReactiveMongoTemplate.class);
    }
}

