package com.loopers.domain.actionlog;

import com.loopers.application.product.UserActionEvent;
import com.loopers.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "user_action_log")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class UserActionLog extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false)
    private ActionType actionType;

    public UserActionLog(Long userId, Long productId, ActionType actionType) {
        this.userId = userId;
        this.productId = productId;
        this.actionType = actionType;
    }

    public static UserActionLog create(UserActionEvent event) {
        return new UserActionLog(event.userId(), event.productId(), event.actionType());
    }
}
