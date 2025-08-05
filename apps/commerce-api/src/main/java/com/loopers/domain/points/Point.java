package com.loopers.domain.points;

import jakarta.persistence.Embeddable;

import java.math.BigDecimal;

@Embeddable
public class Point {
    public BigDecimal amount;
    public Point() {
        this.amount = BigDecimal.ZERO;
    }
    public Point(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal value() {
        return amount;
    }
}
