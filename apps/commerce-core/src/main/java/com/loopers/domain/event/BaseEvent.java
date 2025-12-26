package com.loopers.domain.event;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.Getter;

@Getter
public abstract class BaseEvent {
    private final EventType eventType;

    public BaseEvent(EventType eventType) {
        if (eventType == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이벤트 타입은 필수입니다.");
        }
        this.eventType = eventType;
    }
}
