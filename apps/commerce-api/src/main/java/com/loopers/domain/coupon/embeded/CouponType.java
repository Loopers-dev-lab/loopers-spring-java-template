package com.loopers.domain.coupon.embeded;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public class CouponType {
    @Column(name = "coupon_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private CouponTypeEnum type;

    public CouponType() {

    }
    private CouponType(CouponTypeEnum type) {
        this.type = type;
    }
    public static CouponType of(String type){
        return new CouponType(CouponTypeEnum.from(type));
    }
    public CouponTypeEnum getValue() {
        return this.type;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CouponType)) return false;
        CouponType that = (CouponType) o;
        return type == that.type;
    }

    @Override
    public int hashCode() {
        return type != null ? type.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "CouponType{" + "type=" + type + '}';
    }

    public enum CouponTypeEnum {
        FIXED,   // 정액 할인
        RATE     // 정률 할인
        ;
        public static CouponTypeEnum from(String type){
            if(type == null){
                throw new CoreException(ErrorType.BAD_REQUEST,"Coupon type은 필수 입니다.");
            }
            try {
                return CouponTypeEnum.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new CoreException(ErrorType.BAD_REQUEST, "유효하지 않은 Coupon type 입니다: " + type);
            }
        }

    }
}
