package com.loopers;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.util.TimeZone;

@ConfigurationPropertiesScan
@SpringBootApplication
@EnableScheduling
public class CommerceApiApplication {

    /**
     * Sets the JVM default time zone to Asia/Seoul after the application bean is constructed.
     *
     * This runs after dependency injection so the application (including scheduled tasks) uses
     * Asia/Seoul as the default time zone.
     */
    @PostConstruct
    public void started() {
        // set timezone
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
    }

    public static void main(String[] args) {
        SpringApplication.run(CommerceApiApplication.class, args);
    }
}