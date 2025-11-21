package com.loopers.domain.product;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.common.vo.Price;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

@Entity
@Table(name = "tb_product")
@Getter
public class Product extends BaseEntity {
    protected Product() {
    }

    private String name;
    @Column(name = "brand_id", nullable = false, updatable = false)
    private Long brandId;
    @Convert(converter = Price.Converter.class)
    private Price price;

    public static Product create(String name, Long brandId, Price price) {
        if (StringUtils.isBlank(name)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품명은 필수이며 공백일 수 없습니다.");
        }
        if (brandId == null || brandId <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "브랜드 ID는 1 이상이어야 합니다.");
        }
        if (price == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "가격은 필수입니다.");
        }
        if (price.amount() < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "가격은 음수가 될 수 없습니다.");
        }
        Product product = new Product();
        product.name = name;
        product.brandId = brandId;
        product.price = price;
        return product;
    }
}
