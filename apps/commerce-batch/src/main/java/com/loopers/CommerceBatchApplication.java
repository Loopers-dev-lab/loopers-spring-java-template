package com.loopers;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableBatchProcessing
@SpringBootApplication
public class CommerceBatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommerceBatchApplication.class, args);
    }
}