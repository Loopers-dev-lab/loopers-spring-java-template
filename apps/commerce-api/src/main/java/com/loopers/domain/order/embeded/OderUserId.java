package com.loopers.domain.order.embeded;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Embeddable;

@Embeddable
public class OderUserId {
    private Long userId;

    private OderUserId(Long userId) {
        this.userId = userId;
    }

    public OderUserId() {

    }

    public static OderUserId of(Long userId) {
        if(userId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "userId cannot be null");
        }
        return new OderUserId(userId);
    }

    public Long getValue() {
        return this.userId;
    }
}
