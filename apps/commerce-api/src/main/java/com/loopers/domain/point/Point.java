package com.loopers.domain.point;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.user.User;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "point")
@Getter
public class Point extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Long balance;

    protected Point() {}

    public Point(User user) {
        validateUser(user);
        this.user = user;
        this.balance = 0L;
    }

    private void validateUser(User user) {
        if (user == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용자는 비어있을 수 없습니다.");
        }
    }
}
