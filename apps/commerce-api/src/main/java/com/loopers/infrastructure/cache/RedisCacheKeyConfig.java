package com.loopers.infrastructure.cache;

import java.util.Arrays;

public enum RedisCacheKeyConfig {
    PRODUCT_DETAIL("product:detail", 3600),
    PRODUCT_LIST("product:list", 1800),
    ;

    final String prefix;
    public final int expireSeconds;
    RedisCacheKeyConfig(String prefix, int expireSeconds){
        this.prefix = prefix;
        this.expireSeconds = expireSeconds;
    }

    public String generateKey(Object... args) {
        return prefix + ":" + String.join(":", Arrays.stream(args).map(Object::toString).toArray(String[]::new));
    }
}
