package com.loopers.domain.brand;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.brand.embeded.BrandActive;
import com.loopers.domain.brand.embeded.BrandLikeCount;
import com.loopers.domain.brand.embeded.BrandName;
import com.loopers.domain.brand.embeded.BrandSnsLink;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Entity
@Table(name = "brand")
public class BrandModel extends BaseEntity {

    @Embedded
    private BrandName brandName;
    @Embedded
    private BrandSnsLink snsLink;
    @Embedded
    private BrandLikeCount LikeCount;
    @Embedded
    private BrandActive isActive;

    public BrandModel() {
    }

    private BrandModel(BrandName brandNaem, BrandSnsLink snsLink, BrandLikeCount likeCount, BrandActive isActive) {
        this.brandName = brandNaem;
        this.snsLink = snsLink;
        this.LikeCount = likeCount;
        this.isActive = isActive;
    }
    public static BrandModel of(String brandName, String bradnSnsLink, BigDecimal likeCount, boolean isActive) {
        return new BrandModel(
                BrandName.of(brandName),
                BrandSnsLink.of(bradnSnsLink),
                BrandLikeCount.of(likeCount),
                BrandActive.of(isActive)
        );
    }
    public void incrementLikeCount(){
        this.LikeCount = this.LikeCount.increment();
    }

    public void decrementLikeCount(){
        this.LikeCount = this.LikeCount.decrement();
    }
}
