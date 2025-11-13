package com.loopers.domain.like;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.user.UserModel;
import com.loopers.domain.product.ProductModel;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "like",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "product_id"})
)
public class LikeModel extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserModel user;
    @ManyToOne
    @JoinColumn(name = "product_id")
    private ProductModel product;

    public LikeModel(UserModel user, ProductModel product) {
        this.user = user;
        this.product = product;
    }

    public UserModel getUser() {
        return user;
    }

    public ProductModel getProduct() {
        return product;
    }

}

