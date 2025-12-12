package com.loopers.application.product;

import com.loopers.domain.actionlog.ActionType;

public record UserActionEvent(
        Long userId,
        Long productId,
        ActionType actionType
) {
}
