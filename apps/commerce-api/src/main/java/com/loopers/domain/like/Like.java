package com.loopers.domain.like;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import com.loopers.domain.user.User;
import com.loopers.domain.product.Product;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "like")
public class Like extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne  
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Builder
    public Like(User user, Product product) {
        this.user = user;
        this.product = product;
    }
}
