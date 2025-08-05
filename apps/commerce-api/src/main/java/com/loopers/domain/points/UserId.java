package com.loopers.domain.points;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Embeddable;

@Embeddable
public class UserId {
    private Long userId;

    public UserId(Long userId) {
        this.userId = userId;
    }

    public UserId() {
    }

    public UserId from(Long userId) {
        if(userId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST,"userId cannot be null");
        }
        return new UserId(userId);
    }
    public Long value() {
        return userId;
    }
}
