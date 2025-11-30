package com.loopers.core.domain.payment.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("카드 번호")
class CardNoTest {

    @Nested
    @DisplayName("생성자")
    class Constructor {

        @Nested
        @DisplayName("유효한 값인 경우")
        class 유효한_값인_경우 {

            @Test
            @DisplayName("CardNo 객체를 생성한다")
            void 객체_생성() {
                // given
                String cardNumber = "1234567890123456";

                // when
                CardNo cardNo = new CardNo(cardNumber);

                // then
                assertThat(cardNo.value()).isEqualTo(cardNumber);
            }
        }

        @Nested
        @DisplayName("값이 null인 경우")
        class 값이_null인_경우 {

            @Test
            @DisplayName("NullPointerException을 발생시킨다")
            void 예외_발생() {
                // when & then
                assertThatThrownBy(() -> new CardNo(null))
                        .isInstanceOf(NullPointerException.class)
                        .hasMessage("카드 번호는(은) Null이 될 수 없습니다.");
            }
        }
    }
}
