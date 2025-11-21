package com.loopers.application.order;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class OrderCommand {
    
    private final String memberId;
    private final List<OrderLineCommand> orderLines;
    
    public static OrderCommand of(String memberId, List<OrderLineCommand> orderLines) {
        return OrderCommand.builder()
                .memberId(memberId)
                .orderLines(orderLines)
                .build();
    }
}