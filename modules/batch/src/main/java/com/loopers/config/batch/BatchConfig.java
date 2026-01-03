package com.loopers.config.batch;

import org.springframework.context.annotation.Configuration;

/**
 * Spring Batch Configuration
 *
 * Spring Boot 3.x에서는 @EnableBatchProcessing을 사용하지 않고
 * Auto-configuration을 사용합니다.
 *
 * spring.batch.job.enabled=false로 설정하여 자동 실행을 방지합니다.
 */
@Configuration
public class BatchConfig {

}