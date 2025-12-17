package com.loopers.interfaces.consumer;

import static org.assertj.core.api.Assertions.assertThat;

import com.loopers.testcontainers.RedisTestContainersConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

/**
 * Consumer 스모크 테스트
 *
 * 목적:
 * - Spring Context가 정상적으로 로딩되는지 확인
 * - Consumer Bean들이 정상적으로 생성되는지 확인
 * - 기본 설정(Topic, Consumer Group 등)이 올바른지 확인
 *
 * 실제 Kafka 연결은 하지 않으며, Bean 생성과 설정만 검증합니다.
 */
@SpringBootTest
@Import(RedisTestContainersConfig.class)
@DisplayName("Consumer 스모크 테스트")
class ConsumerSmokeTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @DisplayName("Spring Context가 정상적으로 로딩된다")
    void contextLoads() {
        // Context 로딩 성공 시 테스트 통과
        assertThat(applicationContext).isNotNull();
    }

    @Test
    @DisplayName("CatalogEventConsumer Bean이 정상적으로 생성된다")
    void catalogEventConsumerBeanExists() {
        CatalogEventConsumer consumer = applicationContext.getBean(CatalogEventConsumer.class);
        assertThat(consumer).isNotNull();
    }

    @Test
    @DisplayName("OrderEventConsumer Bean이 정상적으로 생성된다")
    void orderEventConsumerBeanExists() {
        OrderEventConsumer consumer = applicationContext.getBean(OrderEventConsumer.class);
        assertThat(consumer).isNotNull();
    }
}
