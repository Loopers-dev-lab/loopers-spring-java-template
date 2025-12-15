package com.loopers.infrastructure.dataplatform;

/**
 * 데이터 플랫폼 응답
 */
public record DataPlatformResponse(
    boolean success,
    String message
) {}
