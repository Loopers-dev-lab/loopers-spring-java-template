package com.loopers.config.redis;

public record RedisNodeInfo(
        String host,
        int port
) { }
