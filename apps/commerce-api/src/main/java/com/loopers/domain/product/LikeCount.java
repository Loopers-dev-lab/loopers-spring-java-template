package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
public class LikeCount {

    @Column(nullable = false)
    private Integer count;

    @Builder
    private LikeCount(Integer count) {
        validate(count);
        this.count = count;
    }

    public static LikeCount createLikeCount(Integer count) {
        return LikeCount.builder()
                .count(count)
                .build();
    }

    public void increase() {
        this.count++;
    }

    public void decrease() {
        if (this.count <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "좋아요 수는 0보다 작을 수 없습니다.");
        }
        this.count--;
    }

    private void validate(Integer count) {
        if (count == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "좋아요 수는 필수입니다.");
        }
        if (count < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "좋아요 수는 0 이상이어야 합니다.");
        }
    }
}
