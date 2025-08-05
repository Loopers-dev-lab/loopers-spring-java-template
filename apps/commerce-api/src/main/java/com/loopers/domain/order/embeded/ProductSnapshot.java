package com.loopers.domain.order.embeded;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

import java.math.BigDecimal;

@Embeddable
@Getter
public class ProductSnapshot {
    
    @Column(name = "product_name_at_order")
    private String productName;
    
    @Column(name = "option_name_at_order")
    private String optionName;
    
    @Column(name = "image_url_at_order")
    private String imageUrl;
    
    @Column(name = "price_at_order")
    private BigDecimal priceAtOrder;
    
    protected ProductSnapshot() {}
    
    private ProductSnapshot(String productName, String optionName, String imageUrl, BigDecimal priceAtOrder) {
        this.productName = productName;
        this.optionName = optionName;
        this.imageUrl = imageUrl;
        this.priceAtOrder = priceAtOrder;
    }
    
    public static ProductSnapshot of(String productName, String optionName, String imageUrl, BigDecimal priceAtOrder) {
        validateSnapshot(productName, priceAtOrder);
        return new ProductSnapshot(productName, optionName, imageUrl, priceAtOrder);
    }
    
    private static void validateSnapshot(String productName, BigDecimal priceAtOrder) {
        if (productName == null || productName.trim().isEmpty()) {
            throw new IllegalArgumentException("상품명은 필수입니다.");
        }
        if (priceAtOrder == null || priceAtOrder.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("주문 시점 가격은 0 이상이어야 합니다.");
        }
    }
}