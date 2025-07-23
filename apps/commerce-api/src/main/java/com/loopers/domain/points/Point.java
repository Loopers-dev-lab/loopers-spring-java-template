package com.loopers.domain.points;

import jakarta.persistence.Embeddable;
import lombok.Getter;

import java.math.BigDecimal;

@Embeddable
@Getter
public class Point {
    public BigDecimal amount;
    public Point() {
        this.amount = BigDecimal.ZERO;
    }
    public Point(BigDecimal amount) {

        this.amount = amount;
    }
}
