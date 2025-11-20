package com.loopers.domain.product;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.brand.BrandModel;
import com.loopers.domain.product.exception.NotEnoughStockException;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "product")
public class ProductModel extends BaseEntity {

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 32)
    private String category;

    private Integer price;

    private Integer stock;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ProductStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "brand_id", nullable = false)
    private BrandModel brand;

    protected ProductModel() {}

    @Builder
    public ProductModel(String name,
                        String category,
                        Integer price,
                        Integer stock,
                        ProductStatus status,
                        BrandModel brand) {

        validateProductName(name);
        validateProductCategory(category);
        validateProductStatus(status);
        validatePrice(price);
        validateStock(stock);

        this.name = name;
        this.category = category;
        this.price = price;
        this.stock = stock;
        this.status = status;
        this.brand = brand;
    }

    public void decreaseStock(Integer stock) {
        // validateStock(stock);
        if(this.stock < stock) {
            throw new NotEnoughStockException(String.format(
                    "재고가 부족합니다. 현재 재고량 : %d, 입력 재고량 : %d", this.stock, stock));
        }

        this.stock -= stock;
    }

    public void increaseStock(Integer stock) {
        validateStock(stock);
        this.stock += stock;
    }

    public boolean isStatusOnDeletedOrStopSelling() {
        return status == ProductStatus.DELETE || status == ProductStatus.STOP_SELLING;
    }

    private void validateProductCategory(String category) {
        if(category == null || category.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "카테고리명은 필수 입력값 입니다.");
        }

        if(category.length() > 32) {
            throw new CoreException(ErrorType.BAD_REQUEST, "카테고리명은 32자 이하 입력값 입니다.");
        }
    }

    private void validateProductName(String name) {
        if(name == null || name.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품명은 필수 입력값 입니다.");
        }

        if(name.length() > 50) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품명은 50자 이하 입력값이어야 합니다.");
        }
    }

    private void validateProductStatus(ProductStatus status) {
        if(status == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상태값 필수 입력");
        }
    }

    private void validatePrice(Integer price) {
        if(price == null || price < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품 금액은 null이거나 음수일 수 없습니다.");
        }
    }

    private void validateStock(Integer stock) {
        if(stock == null || stock < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "입력된 재고는 null이거나 음수일 수 없습니다.");
        }
    }

}
