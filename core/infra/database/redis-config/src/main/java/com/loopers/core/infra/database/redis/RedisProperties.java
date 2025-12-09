package com.loopers.core.infra.database.redis;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(value = "datasource.redis")
public record RedisProperties(
        int database,
        RedisNodeInfo master,
        List<RedisNodeInfo> replicas
) {
}
