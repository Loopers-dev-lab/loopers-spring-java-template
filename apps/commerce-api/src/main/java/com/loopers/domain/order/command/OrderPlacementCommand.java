package com.loopers.domain.order.command;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class OrderPlacementCommand {
    
    private final String memberId;
    private final List<OrderLineCommand> orderLines;
    
    public static OrderPlacementCommand of(String memberId, List<OrderLineCommand> orderLines) {
        return OrderPlacementCommand.builder()
                .memberId(memberId)
                .orderLines(orderLines)
                .build();
    }
}
