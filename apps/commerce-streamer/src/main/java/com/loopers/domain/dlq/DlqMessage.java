package com.loopers.domain.dlq;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

/**
 * Dead Letter Queue Message
 * - Consumer에서 처리 실패한 메시지를 저장
 * - 디버깅 및 재처리를 위한 이력 보관
 */
@Entity
@Table(name = "dlq_message", indexes = {
    @Index(name = "idx_original_topic", columnList = "original_topic"),
    @Index(name = "idx_failed_at", columnList = "failed_at"),
    @Index(name = "idx_error_type", columnList = "error_type")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DlqMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 원본 토픽 이름
     */
    @Column(name = "original_topic", nullable = false, length = 200)
    private String originalTopic;

    /**
     * 원본 파티션
     */
    @Column(name = "original_partition")
    private Integer originalPartition;

    /**
     * 원본 오프셋
     */
    @Column(name = "original_offset")
    private Long originalOffset;

    /**
     * 메시지 키
     */
    @Column(name = "message_key", length = 200)
    private String messageKey;

    /**
     * 메시지 값 (원본 JSON)
     */
    @Column(name = "message_value", columnDefinition = "TEXT", nullable = false)
    private String messageValue;

    /**
     * 에러 타입
     */
    @Column(name = "error_type", nullable = false, length = 100)
    private String errorType;

    /**
     * 에러 메시지
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * 스택 트레이스
     */
    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;

    /**
     * 실패 시각
     */
    @Column(name = "failed_at", nullable = false)
    private ZonedDateTime failedAt;

    /**
     * 재처리 완료 여부
     */
    @Column(name = "resolved", nullable = false)
    private boolean resolved = false;

    /**
     * DLQ 메시지 생성
     */
    public static DlqMessage create(
            String originalTopic,
            Integer originalPartition,
            Long originalOffset,
            String messageKey,
            String messageValue,
            Exception exception
    ) {
        DlqMessage dlqMessage = new DlqMessage();
        dlqMessage.originalTopic = originalTopic;
        dlqMessage.originalPartition = originalPartition;
        dlqMessage.originalOffset = originalOffset;
        dlqMessage.messageKey = messageKey;
        dlqMessage.messageValue = messageValue;
        dlqMessage.errorType = resolveErrorType(exception);
        dlqMessage.errorMessage = exception.getMessage();
        dlqMessage.stackTrace = getStackTraceAsString(exception);
        dlqMessage.failedAt = ZonedDateTime.now();
        dlqMessage.resolved = false;
        return dlqMessage;
    }

    /**
     * 재처리 완료 처리
     */
    public void markAsResolved() {
        this.resolved = true;
    }

    /**
     * 스택 트레이스를 문자열로 변환
     */
    private static String getStackTraceAsString(Exception exception) {
        StringBuilder sb = new StringBuilder();
        sb.append(exception.toString()).append("\n");

        StackTraceElement[] stackTrace = exception.getStackTrace();
        int maxLines = Math.min(stackTrace.length, 10); // 최대 10줄만 저장

        for (int i = 0; i < maxLines; i++) {
            sb.append("\tat ").append(stackTrace[i]).append("\n");
        }

        if (stackTrace.length > maxLines) {
            sb.append("\t... ").append(stackTrace.length - maxLines).append(" more");
        }

        return sb.toString();
    }

    private static String resolveErrorType(Exception exception) {
        String simpleName = exception.getClass().getSimpleName();
        if (!simpleName.isEmpty()) {
            return simpleName;
        }
        // 익명 클래스의 경우 simpleName이 빈 문자열이므로 부모 클래스 이름 사용
        Class<?> superClass = exception.getClass().getSuperclass();
        if (superClass != null) {
            return superClass.getSimpleName();
        }
        return exception.getClass().getName();
    }
}
