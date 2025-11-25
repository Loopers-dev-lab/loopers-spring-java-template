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
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
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
public class UserPointEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public static UserPointEntity from(UserPoint userPoint) {
        return new UserPointEntity(
                Optional.ofNullable(userPoint.getId().value())
                        .map(Long::parseLong)
                        .orElse(null),
                Objects.requireNonNull(
                        Optional.ofNullable(userPoint.getUserId().value())
                                .map(Long::parseLong)
                                .orElse(null)
                ),
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
