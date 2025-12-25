package com.loopers;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class CommerceCollectorApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommerceCollectorApplication.class, args);
    }

}
