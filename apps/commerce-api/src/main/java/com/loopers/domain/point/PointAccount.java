package com.loopers.domain.point;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "point_accounts")
public class PointAccount extends BaseEntity {

    private String userId;

    @Embedded
    private Point balance = Point.zero();

    protected PointAccount() {
    }

    protected PointAccount(String userId) {
        this.userId = userId;
    }

    public static PointAccount create(String userId) {
        return new PointAccount(userId);
    }

    public String getUserId() {
        return userId;
    }

    public Point getBalance() {
        return balance;
    }

}
