package com.br.webfluxreativo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import com.br.webfluxreativo.repository.UserRepository;

// Keep the autoconfig exclusion if you don't want the real Mongo auto-config to run
// during tests, but import TestMongoConfig to provide a mock ReactiveMongoTemplate
@SpringBootTest(properties = {"spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration,org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration"})
@Import(TestMongoConfig.class)
class WebfluxReativoApplicationTests {

    @MockBean
    private UserRepository userRepository;

    @Test
    void contextLoads() {
    }

}
