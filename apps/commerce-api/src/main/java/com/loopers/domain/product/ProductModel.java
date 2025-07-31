package com.loopers.domain.product;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.product.embeded.*;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

import java.math.BigDecimal;

@Entity@Getter
@Table(name = "product")
public class ProductModel extends BaseEntity {
    @Embedded
    private ProductName productName;
    @Embedded
    private BrandId brandId;
    @Embedded
    private ProductStock stock;
    @Embedded
    private ProductPrice price;
    @Embedded
    private ProductDscription description;
    @Embedded
    private ProductImgUrl imgUrl;
    @Embedded
    private ProductStatus Status;
    @Embedded
    private ProductLikeCount LikeCount;
    

    public ProductModel() {

    }

    public ProductModel(ProductName productName, BrandId brandId, ProductStock stock, ProductPrice price, ProductDscription description, ProductImgUrl imgUrl, ProductStatus status, ProductLikeCount likeCount) {
        this.productName = productName;
        this.brandId = brandId;
        this.stock = stock;
        this.price = price;
        this.description = description;
        this.imgUrl = imgUrl;
        this.Status = status;
        this.LikeCount = likeCount;
    }

    public static ProductModel register(String productName, Long brandId, BigDecimal stock, BigDecimal productPrice, String productDescription, String productImgUrl, String productStatus, BigDecimal productLikeCount) {
        return new ProductModel(
                ProductName.of(productName),
                BrandId.of(brandId),
                ProductStock.of(stock),
                ProductPrice.of(productPrice),
                ProductDscription.of(productDescription),
                ProductImgUrl.of(productImgUrl),
                ProductStatus.of(productStatus),
                ProductLikeCount.of(productLikeCount)
        );
    }
    public void decreaseStock(BigDecimal quantity) {
        this.stock = this.stock.decrease(quantity);
    }

    public void restoreStock(BigDecimal quantity) {
        this.stock = this.stock.increase(quantity);
    }

    public boolean hasEnoughStock(BigDecimal quantity) {
        if (this.stock == null) {
            return false;
        }
        return this.stock.hasEnough(quantity);
    }
    public void incrementLikeCount(){
        this.LikeCount = this.LikeCount.increment();
    }
    
    public void decrementLikeCount(){
        this.LikeCount = this.LikeCount.decrement();
    }
    public boolean isAvailable(){
        return this.Status.isAvailable() && hasEnoughStock(BigDecimal.ONE);
    }



}
