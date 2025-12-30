package com.loopers.testcontainers;

import com.redis.testcontainers.RedisContainer;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.utility.DockerImageName;

@Configuration
public class RedisTestContainersConfig {
    private static final RedisContainer redisContainer = new RedisContainer(DockerImageName.parse("redis:latest"));

    static {
        redisContainer.start();
        
        // System Property는 static 블록에서 설정해야 Spring Boot가 YAML을 읽기 전에 적용됨
        System.setProperty("REDIS_MASTER_HOST", redisContainer.getHost());
        System.setProperty("REDIS_MASTER_PORT", String.valueOf(redisContainer.getFirstMappedPort()));
        System.setProperty("REDIS_REPLICA_1_HOST", redisContainer.getHost());
        System.setProperty("REDIS_REPLICA_1_PORT", String.valueOf(redisContainer.getFirstMappedPort()));
    }
}
