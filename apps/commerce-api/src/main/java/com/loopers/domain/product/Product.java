package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.Getter;

/**
 * packageName : com.loopers.domain.product
 * fileName     : Product
 * author      : byeonsungmun
 * date        : 2025. 11. 10.
 * description :
 * ===========================================
 * DATE         AUTHOR       NOTE
 * -------------------------------------------
 * 2025. 11. 10.     byeonsungmun       최초 생성
 */

@Entity
@Table(name = "product")
@Getter
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ref_brand_id", nullable = false)
    private Long brandId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Long price;

    @Column
    private Long likeCount;

    @Column(nullable = false)
    private Long stock;

    @Version
    private Long version;

    protected Product() {}

    private Product(Long brandId, String name, Long price, Long likeCount, Long stock) {
        this.brandId = requireValidBrandId(brandId);
        this.name = requireValidName(name);
        this.price = requireValidPrice(price);
        this.likeCount = requireValidLikeCount(likeCount);
        this.stock = requireValidStock(stock);
    }

    public static Product create(Long brandId, String name, Long price, Long stock) {
        return new Product(
                brandId,
                name,
                price,
                0L,
                stock
        );
    }

    private Long requireValidBrandId(Long brandId) {
        if (brandId == null || brandId <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "브랜드 ID는 필수입니다.");
        }

        return brandId;
    }

    private String requireValidName(String name) {
        if (name == null || name.isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품명은 필수입니다.");
        }
        return name;
    }

    private Long requireValidPrice(Long price) {
        if (price == null || price < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품 가격은 0원 이상이어야 합니다.");
        }
        return price;
    }

    public Long requireValidLikeCount(Long likeCount) {
        if (likeCount == null || likeCount < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "좋아요 개수는 0개 미만으로 설정할 수 없습니다.");
        }
        return likeCount;
    }

    private Long requireValidStock(Long stock) {
        if (stock < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품 재고는 0 미만으로 설정할 수 없습니다.");
        }
        return stock;
    }

    public void increaseLikeCount() {
        this.likeCount++;
    }

    public void decreaseLikeCount() {
        if (this.likeCount > 0) this.likeCount--;
    }

    public void decreaseStock(Long quantity) {
        if (quantity <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "차감 수량은 0보다 커야 합니다.");
        }
        if (this.stock - quantity < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "재고가 부족합니다.");
        }
        this.stock -= quantity;
    }
}
