package com.loopers.domain.order;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * packageName : com.loopers.domain.order
 * fileName     : OrderItem
 * author      : byeonsungmun
 * date        : 2025. 11. 11.
 * description :
 * ===========================================
 * DATE         AUTHOR       NOTE
 * -------------------------------------------
 * 2025. 11. 11.     byeonsungmun       최초 생성
 */

@Entity
@Table(name = "order_item")
@Getter
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "ref_product_id", nullable = false)
    private Long productId;

    @Column(name = "ref_product_name", nullable = false)
    private String productName;

    @Column(nullable = false)
    private Long quantity;

    @Column(nullable = false)
    private Long price;

    protected OrderItem() {}

    private OrderItem(Long productId, String productName, Long quantity, Long price) {
        this.productId = requiredValidProductId(productId);
        this.productName = requiredValidProductName(productName);
        this.quantity = requiredQuantity(quantity);
        this.price = requiredPrice(price);
    }

    public static OrderItem create(Long productId, String productName, Long quantity, Long price) {
        return new OrderItem(productId, productName, quantity, price);
    }

    public Long getAmount() {
        return quantity * price;
    }

    private Long requiredValidProductId(Long productId) {
        if (productId == null || productId <= 0)  {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품 ID는 필수입니다.");
        }
        return productId;
    }

    private String requiredValidProductName(String productName) {
        if (productName == null || productName.isEmpty())  {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품명은 필수입니다.");
        }
        return productName;
    }

    private Long requiredQuantity(Long quantity) {
        if (quantity == null || quantity <= 0)  {
            throw new CoreException(ErrorType.BAD_REQUEST, "수량은 1개 이상이어야 합니다.");
        }
        return quantity;
    }

    private Long requiredPrice(Long price) {
        if (price == null || price < 0)  {
            throw new CoreException(ErrorType.BAD_REQUEST, "가격은 0원 이상이어야 합니다.");
        }
        return price;
    }
}
