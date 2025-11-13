package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Embeddable;

@Embeddable
public class UserId {
    private String userId;

    protected UserId() {
    }

    public UserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "UserId는 비어있을 수 없습니다.");
        }
        if (!userId.matches("^[a-zA-Z0-9_-]{1,10}$")) {
            throw new CoreException(ErrorType.BAD_REQUEST, "ID 가 `영문 및 숫자 10자 이내` 형식에 맞아야 합니다.");
        }
        this.userId = userId;
    }

    public String userId() {
        return userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserId userId1 = (UserId) o;
        return userId != null ? userId.equals(userId1.userId) : userId1.userId == null;
    }

    @Override
    public int hashCode() {
        return userId != null ? userId.hashCode() : 0;
    }
}
