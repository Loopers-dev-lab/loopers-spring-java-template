package com.loopers.domain.like;

import com.loopers.domain.product.ProductModel;
import com.loopers.domain.user.UserModel;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "product_like",
    uniqueConstraints =
        @UniqueConstraint(name="uk_like_user_product",
        columnNames = {"user_id", "product_id"})
)
public class ProductLikeModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductModel product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserModel user;

    @CreationTimestamp
    private LocalDateTime createdAt;

    protected ProductLikeModel() {}

    public static ProductLikeModel of(UserModel u, ProductModel p) {
        ProductLikeModel pl = new ProductLikeModel();
        pl.user = u; pl.product = p;
        return pl;
    }

}
