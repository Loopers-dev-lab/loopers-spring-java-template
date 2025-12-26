package com.loopers.infrastructure.dlq;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * DLQ에 저장되는 메시지 구조
 * 
 * 원본 메시지와 함께 실패 원인, 재시도 횟수 등의 메타데이터를 포함합니다.
 */
@Getter
@Builder
public class DlqMessage {
    /**
     * 원본 토픽 이름
     */
    private String originalTopic;

    /**
     * 원본 메시지의 키 (partition key)
     */
    private String originalKey;

    /**
     * 원본 메시지 (JSON 문자열 또는 Map)
     */
    private Object originalMessage;

    /**
     * 에러 메시지
     */
    private String errorMessage;

    /**
     * 재시도 횟수
     */
    private Integer retryCount;

    /**
     * 실패 시각 (ISO-8601 형식)
     */
    private String failedAt;
}

