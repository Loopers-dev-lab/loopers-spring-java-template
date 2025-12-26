package com.loopers;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.TimeZone;

@SpringBootApplication
@EntityScan(basePackages = {
    "com.loopers.domain",
    "com.loopers.infrastructure.idempotency"  // EventHandled 엔티티
})
@EnableJpaRepositories(basePackages = {
    "com.loopers.infrastructure.brand",
    "com.loopers.infrastructure.coupon",
    "com.loopers.infrastructure.example",
    "com.loopers.infrastructure.idempotency",  // EventHandled Repository
    "com.loopers.infrastructure.like",
    "com.loopers.infrastructure.metrics",
    "com.loopers.infrastructure.order",
    "com.loopers.infrastructure.point",
    "com.loopers.infrastructure.product",
    "com.loopers.infrastructure.supply",
    "com.loopers.infrastructure.user",
    "com.loopers.infrastructure.event.outbox"  // Outbox Repository
})
public class CommerceCoreApplication {

    @PostConstruct
    public void started() {
        // set timezone
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
    }

    public static void main(String[] args) {
        SpringApplication.run(CommerceCoreApplication.class, args);
    }
}

