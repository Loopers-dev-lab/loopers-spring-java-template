package com.loopers.domain.order;

import com.loopers.domain.common.vo.Price;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

@Embeddable
@Getter
public class OrderItem {
    private Long productId;
    private String productName;
    private Integer quantity;
    @Convert(converter = Price.Converter.class)
    private Price pricePerItem;

    public Integer getTotalPrice() {
        return this.pricePerItem.amount() * this.quantity;
    }

    protected OrderItem() {
    }

    private OrderItem(Long productId, String productName, Integer quantity, Price pricePerItem) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.pricePerItem = pricePerItem;
    }

    public static OrderItem create(Long productId, String productName, Integer quantity, Price price) {
        if (StringUtils.isBlank(productName)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품명은 필수이며 공백일 수 없습니다.");
        }
        if (quantity == null || quantity <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 수량은 1 이상의 자연수여야 합니다.");
        }
        if (price == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "가격은 필수입니다.");
        }

        return new OrderItem(productId, productName, quantity, price);
    }
}
