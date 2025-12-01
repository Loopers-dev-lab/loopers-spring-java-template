package com.loopers.domain.cache;

import java.time.Duration;

public enum CachePolicy {

  PRODUCT("product", Duration.ofMinutes(5)),
  BRAND("brand", Duration.ofHours(6)),
  PRODUCT_LIST("product:list", Duration.ofMinutes(1));

  private static final String VERSION = "v1";

  private final String prefix;
  private final Duration ttl;

  CachePolicy(String prefix, Duration ttl) {
    this.prefix = prefix;
    this.ttl = ttl;
  }

  public Duration getTtl() {
    return ttl;
  }

  public String buildKey(Long id) {
    return prefix + ":" + VERSION + ":" + id;
  }

  public String buildKey(Long brandId, String sortType) {
    String brandKey = brandId != null ? String.valueOf(brandId) : "all";
    return prefix + ":" + VERSION + ":" + sortType + ":" + brandKey;
  }
}
