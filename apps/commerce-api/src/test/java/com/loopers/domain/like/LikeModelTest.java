package com.loopers.domain.like;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import com.loopers.domain.user.UserModel;
import com.loopers.domain.user.UserId;
import com.loopers.domain.user.Email;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.BirthDate;
import com.loopers.domain.product.ProductModel;
import com.loopers.domain.product.Brand;
import com.loopers.domain.common.Money;
import com.loopers.domain.common.Quantity;

class LikeModelTest {
    @DisplayName("좋아요 행위 ")
    @Nested
    class Create {
        @DisplayName("좋아요 등록이 정상 처리된다")
        @Test
        void createsLikeModel_whenLikeIsCreated() {
            // arrange
            UserModel user = new UserModel(new UserId("user123"), new Email("user123@user.com"), new Gender("male"), new BirthDate("1999-01-01"));
            ProductModel product = new ProductModel("product123", new Brand("Apple"), new Money(10000), new Quantity(100));

            // act
            LikeModel likeModel = new LikeModel(user, product);

            // assert
            assertAll(
                () -> assertThat(likeModel.getId()).isNotNull(),
                () -> assertThat(likeModel.getUser()).isEqualTo(user),
                () -> assertThat(likeModel.getProduct()).isEqualTo(product)
            );
        }
    }
}
