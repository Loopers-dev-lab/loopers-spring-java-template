package com.loopers.domain.like;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Product;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LikeTest {

    @DisplayName("사용자와 상품으로 좋아요를 생성할 수 있다.")
    @Test
    void createLike() {
        // arrange
        User dummyUser = User.create("testuser", "test@mail.com", "1990-01-01", Gender.MALE);
        Brand dummyBrand = Brand.create("Dummy Brand");
        Product dummyProduct = Product.create("Test Product", 1000L, 10, dummyBrand);

        // act
        Like like = Like.create(dummyUser, dummyProduct);

        // assert
        assertThat(like.getUser()).isEqualTo(dummyUser);
        assertThat(like.getProduct()).isEqualTo(dummyProduct);
    }
}
