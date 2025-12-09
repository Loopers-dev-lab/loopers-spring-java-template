package com.loopers.core.infra.database.redis;

public record RedisNodeInfo(
        String host,
        int port
) {
}
