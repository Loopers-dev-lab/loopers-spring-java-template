package com.loopers.domain.like;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static java.time.LocalDateTime.now;

@Getter
@Entity
@Table(
        name = "likes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "user_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Like {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    private LocalDateTime deletedAt;    // soft-delete

    public Like(Long userId, Long productId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("유저 ID는 음수 혹은 null일 수 없습니다.");
        }
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("상품 ID는 음수 혹은 null일 수 없습니다.");
        }
        this.userId =  userId;
        this.productId = productId;
        this.deletedAt = null;
    }

    public static Like create(Long userId, Long productId) {
        return new Like(userId, productId);
    }

    public void like() {
        if (this.deletedAt != null) {
            this.deletedAt = null;
        }
    }

    public void unlike() {
        if (this.deletedAt == null) {
            this.deletedAt = now();
        }
    }

    public boolean isActive() {
        return this.deletedAt == null;
    }
}
