package com.loopers.domain.supply;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.supply.vo.Stock;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tb_supply")
@Getter
public class Supply extends BaseEntity {
    private Long productId;
    @Setter
    @Convert(converter = Stock.Converter.class)
    private Stock stock;
    // think: 인당 구매제한?

    protected Supply() {
    }

    public static Supply create(Long productId, Stock stock) {
        if (productId == null || productId <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품 ID는 1 이상이어야 합니다.");
        }
        if (stock == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "재고는 필수입니다.");
        }
        Supply supply = new Supply();
        supply.productId = productId;
        supply.stock = stock;
        return supply;
    }

    // decreaseStock, increaseStock
    public void decreaseStock(int quantity) {
        this.stock = this.stock.decrease(quantity);
    }
}
