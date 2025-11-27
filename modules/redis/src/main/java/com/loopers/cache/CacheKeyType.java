package com.loopers.cache;

import java.time.Duration;

public enum CacheKeyType {

  PRODUCT("product", 5),
  BRAND("brand", 360),  // 6시간
  PRODUCT_LIST("product:list", 1);

  private static final String VERSION = "v1";

  private final String prefix;
  private final Duration ttl;

  CacheKeyType(String prefix, long ttlMinutes) {
    this.prefix = prefix;
    this.ttl = Duration.ofMinutes(ttlMinutes);
  }

  public String buildKey(Long id) {
    return prefix + ":" + VERSION + ":" + id;
  }

public Duration getTtl() {
    return ttl;
  }
}
