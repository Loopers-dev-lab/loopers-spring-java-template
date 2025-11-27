package com.loopers.cache;

import java.time.Duration;

public interface CacheKey<T> {

  String key();

  Duration ttl();

  Class<T> type(); //역직렬화 타입
}
