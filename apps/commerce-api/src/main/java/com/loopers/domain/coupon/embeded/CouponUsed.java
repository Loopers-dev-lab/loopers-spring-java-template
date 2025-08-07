package com.loopers.domain.coupon.embeded;


import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class CouponUsed {

    @Column(name = "is_used", nullable = false)
    private boolean used;

    public CouponUsed() {
    }

    private CouponUsed(boolean used) {
        this.used = used;
    }

    public static CouponUsed of(boolean used) {
        return new CouponUsed(used);
    }

    public boolean isUsed() {
        return this.used;
    }

    public CouponUsed markUsed() {
        return new CouponUsed(true);
    }

    public CouponUsed markUnused() {
        return new CouponUsed(false);
    }

}
