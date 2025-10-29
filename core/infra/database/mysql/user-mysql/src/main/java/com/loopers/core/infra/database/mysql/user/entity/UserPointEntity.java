package com.loopers.core.infra.database.mysql.user.entity;

import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.common.vo.UpdatedAt;
import com.loopers.core.domain.user.UserPoint;
import com.loopers.core.domain.user.vo.UserId;
import com.loopers.core.domain.user.vo.UserPointBalance;
import com.loopers.core.domain.user.vo.UserPointId;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Optional;

@Entity
@Table(
        name = "user_point",
        indexes = {
                @Index(name = "idx_user_point_user_id", columnList = "user_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class UserPointEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private int balance;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static UserPointEntity from(UserPoint userPoint) {
        return new UserPointEntity(
                Optional.ofNullable(userPoint.getId().value())
                        .map(Long::parseLong)
                        .orElse(null),
                Long.parseLong(userPoint.getUserId().value()),
                userPoint.getBalance().value(),
                userPoint.getCreatedAt().value(),
                userPoint.getUpdatedAt().value()
        );
    }

    public UserPoint to() {
        return UserPoint.mappedBy(
                new UserPointId(this.id.toString()),
                new UserId(this.userId.toString()),
                new UserPointBalance(this.balance),
                new CreatedAt(this.createdAt),
                new UpdatedAt(this.updatedAt)
        );
    }
}
