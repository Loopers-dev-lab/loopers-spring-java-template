package com.loopers;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@ConfigurationPropertiesScan
@SpringBootApplication(scanBasePackages = {
    "com.loopers.infrastructure.listener",    // streamer 전용 리스너
    "com.loopers.infrastructure.dlq",        // streamer 전용 DLQ
    "com.loopers.domain",                     // 🆕 core 앱의 도메인 스캔 (모든 도메인 서비스)
    "com.loopers.infrastructure",             // 🆕 core 앱의 모든 infrastructure 컴포넌트 스캔 (Repository 구현체, Service 등)
    "com.loopers.config",                     // JPA 모듈의 JpaConfig, DataSourceConfig 등 설정 클래스 스캔
    "com.loopers.confg"                       // Kafka 모듈의 설정 클래스 스캔 (KafkaConfig)
})
@EnableScheduling
// @EntityScan은 JpaConfig에서 이미 com.loopers 전체를 스캔하므로 불필요
// @EnableJpaRepositories도 JpaConfig에서 이미 com.loopers.infrastructure를 스캔하므로 불필요
public class CommerceStreamerApplication {
    @PostConstruct
    public void started() {
        // set timezone
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
    }

    public static void main(String[] args) {
        SpringApplication.run(CommerceStreamerApplication.class, args);
    }
}


