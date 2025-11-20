package com.loopers.domain.product.vo;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Embeddable;
import lombok.Getter;

@Getter
@Embeddable
public class LikeCount {

    private final int value;

    public LikeCount() { this.value = 0; }

    public LikeCount(int value) {
        if (value < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "좋아요 수는 0 이상이어야 합니다.");
        }
        this.value = value;
    }

    public int value() {
        return value;
    }

    public LikeCount increase(int amount) {
        if (amount < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "증가할 좋아요 수는 음수일 수 없습니다.");
        }
        return new LikeCount(this.value + amount);
    }

    public LikeCount decrease(int amount) {
        if (amount < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "감소할 좋아요 수는 음수일 수 없습니다.");
        }
        if (this.value - amount < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "좋아요 수는 0보다 작을 수 없습니다.");
        }
        return new LikeCount(this.value - amount);
    }
}
