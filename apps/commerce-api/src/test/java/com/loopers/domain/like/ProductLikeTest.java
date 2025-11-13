package com.loopers.domain.like;

import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

public class ProductLikeTest {

    private static final Long USER_ID = 1L;
    private static final Long PRODUCT_ID = 1L;

    @DisplayName("좋아요 객체 생성")
    @Nested
    class Like {

        @DisplayName("사용자 ID와 상품 ID가 주어지면, 좋아요가 객체가 생성된다")
        @Test
        void likeTest1() {
            ProductLike like = ProductLike.create(USER_ID, PRODUCT_ID);

            assertAll(
                    () -> assertThat(like.getUserId()).isEqualTo(USER_ID),
                    () -> assertThat(like.getProductId()).isEqualTo(PRODUCT_ID),
                    () -> assertThat(like.getDeletedAt()).isNull()
            );
        }

        @DisplayName("사용자 ID가 null이면 예외가 발생한다.")
        @Test
        void likeTest2() {
            assertThatThrownBy(() -> ProductLike.create(null, PRODUCT_ID))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("사용자 ID는 필수입니다");
        }

        @DisplayName("상품 ID가 null이면 예외가 발생한다.")
        @Test
        void likeTest3() {
            assertThatThrownBy(() -> ProductLike.create(USER_ID, null))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("상품 ID는 필수입니다");
        }
    }
}
