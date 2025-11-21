package com.loopers.domain.like;

import com.loopers.domain.product.Product;
import com.loopers.domain.user.User;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@NoArgsConstructor
@Entity
@Table(
    name = "product_like",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_product_like_user_product",
            columnNames = {"user_id", "product_id"}
        )
    }
)
@Getter
public class ProductLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private final Long id = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User likeUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", referencedColumnName = "id")
    private Product likeProduct;

    @Column(name = "like_at", nullable = false, updatable = false)
    private ZonedDateTime likeAt;

    @PrePersist
    private void prePersist() {
        this.likeAt = ZonedDateTime.now();
    }

    @Builder
    private ProductLike(User likeUser, Product likeProduct) {

        validateUser(likeUser);

        validateProduct(likeProduct);

        this.likeUser = likeUser;
        this.likeProduct = likeProduct;
    }

    public static ProductLike addLike(User user, Product product) {
        return ProductLike.builder()
                .likeUser(user)
                .likeProduct(product)
                .build();
    }

    public boolean isSameUserAndProduct(User user, Product product) {
        return this.likeUser.equals(user) && this.likeProduct.equals(product);
    }

    private static void validateProduct(Product likeProduct) {
        if( likeProduct == null ) {
            throw new CoreException(ErrorType.BAD_REQUEST, "좋아요를 누를 상품이 필요합니다");
        }
    }

    private static void validateUser(User likeUser) {
        if( likeUser == null ) {
            throw new CoreException(ErrorType.BAD_REQUEST, "좋아요를 누를 사용자가 필요합니다");
        }
    }
}
