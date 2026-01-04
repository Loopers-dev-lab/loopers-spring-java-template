package com.loopers;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Spring Batch 애플리케이션 메인 클래스.
 * <p>
 * 대량 데이터 집계 및 배치 처리를 위한 독립 실행형 애플리케이션입니다.
 * </p>
 * <p>
 * <b>실행 방법:</b>
 * <pre>
 * java -jar commerce-batch.jar \
 *   --spring.batch.job.names=productMetricsAggregationJob \
 *   targetDate=20241215
 * </pre>
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
@SpringBootApplication(scanBasePackages = "com.loopers")
@EnableJpaRepositories(basePackages = "com.loopers.infrastructure")
@EntityScan(basePackages = "com.loopers.domain")
public class BatchApplication {

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(BatchApplication.class, args)));
    }
}

