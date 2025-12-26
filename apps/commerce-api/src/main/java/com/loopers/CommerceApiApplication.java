package com.loopers;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@ConfigurationPropertiesScan
@SpringBootApplication(
        scanBasePackages = {
                "com.loopers.application",  // api 앱의 application 레이어
                "com.loopers.infrastructure",  // api 앱의 infrastructure (OutboxPublisher, PG 등)
                "com.loopers.interfaces",  // api 앱의 인터페이스 (Controller 등)
                "com.loopers.domain",  // 🆕 core 앱의 도메인 스캔 (Service, Repository 인터페이스 등)
                "com.loopers.config"  // JPA 모듈의 JpaConfig, DataSourceConfig 등 설정 클래스 스캔
        },
        exclude = {DataSourceAutoConfiguration.class}  // 커스텀 DataSource 설정 사용을 위해 자동 설정 제외
)
// @EntityScan은 JpaConfig에서 이미 com.loopers 전체를 스캔하므로 불필요
// @EnableJpaRepositories도 JpaConfig에서 이미 com.loopers.infrastructure를 스캔하므로 불필요
@EnableFeignClients
@EnableScheduling
public class CommerceApiApplication {

    @PostConstruct
    public void started() {
        // set timezone
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
    }

    public static void main(String[] args) {
        SpringApplication.run(CommerceApiApplication.class, args);
    }
}
