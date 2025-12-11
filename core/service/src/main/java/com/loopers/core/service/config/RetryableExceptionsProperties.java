package com.loopers.core.service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "retry.exceptions")
public class RetryableExceptionsProperties {

    private List<String> retryable;

    public boolean isRetryable(Exception exception) {
        return retryable.stream()
                .map(this::resolveExceptionClass)
                .anyMatch(exceptionClass -> exceptionClass.isInstance(exception));
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Exception> resolveExceptionClass(String className) {
        try {
            return (Class<? extends Exception>) Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("예외 클래스를 찾을 수 없습니다: " + className, e);
        }
    }
}
