package com.br.webfluxreativo;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class WebfluxReativoApplication {

    private static final Logger log = LoggerFactory.getLogger(WebfluxReativoApplication.class);

    @Autowired
    private Environment env;

    public static void main(String[] args) {
        SpringApplication.run(WebfluxReativoApplication.class, args);
    }

    @PostConstruct
    public void printEnv() {
        log.info("===== CONFIGURAÇÃO INICIAL =====");
        log.info("spring.data.mongodb.uri = {}", env.getProperty("spring.data.mongodb.uri"));
        log.info("spring.data.mongodb.host = {}", env.getProperty("spring.data.mongodb.host"));
        log.info("spring.data.mongodb.port = {}", env.getProperty("spring.data.mongodb.port"));
        log.info("SPRING_DATA_MONGODB_URI = {}", env.getProperty("SPRING_DATA_MONGODB_URI"));
        log.info("server.port = {}", env.getProperty("server.port"));
        log.info("================================");
    }

}
