package com.loopers.config;

import com.loopers.kafka.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    // Consumer concurrency와 동일하게 3개 파티션으로 설정
    private static final int PARTITION_COUNT = 3;

    // local 환경이므로 1 (프로덕션에서는 3 권장)
    private static final int REPLICATION_FACTOR = 1;

    /**
     * 상품 좋아요 이벤트 토픽
     * - 이벤트: LIKE_ADDED, LIKE_REMOVED
     * - Consumer: commerce-collector
     */
    @Bean
    public NewTopic productLikeTopic() {
        return TopicBuilder.name(KafkaTopics.PRODUCT_LIKE)
                .partitions(PARTITION_COUNT)
                .replicas(REPLICATION_FACTOR)
                .build();
    }

    /**
     * 주문 이벤트 토픽
     * - 이벤트: ORDER_CREATED, ORDER_COMPLETED, ORDER_CANCELLED
     * - Consumer: commerce-collector
     */
    @Bean
    public NewTopic orderTopic() {
        return TopicBuilder.name(KafkaTopics.ORDER)
                .partitions(PARTITION_COUNT)
                .replicas(REPLICATION_FACTOR)
                .build();
    }

    /**
     * 사용자 활동 이벤트 토픽
     * - 이벤트: USER_ACTIVITY
     * - Consumer:
     */
    @Bean
    public NewTopic userActivityTopic() {
        return TopicBuilder.name(KafkaTopics.USER_ACTIVITY)
                .partitions(PARTITION_COUNT)
                .replicas(REPLICATION_FACTOR)
                .build();
    }

    /**
     * 쿠폰 이벤트 토픽
     * - 이벤트: COUPON_USED, COUPON_EXPIRED
     * - Consumer:
     */
    @Bean
    public NewTopic couponTopic() {
        return TopicBuilder.name(KafkaTopics.COUPON)
                .partitions(PARTITION_COUNT)
                .replicas(REPLICATION_FACTOR)
                .build();
    }
}
