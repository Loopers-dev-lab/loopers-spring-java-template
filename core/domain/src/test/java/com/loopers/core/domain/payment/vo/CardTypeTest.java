package com.loopers.core.domain.payment.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("카드 유형")
class CardTypeTest {

    @Nested
    @DisplayName("생성자")
    class Constructor {

        @Nested
        @DisplayName("유효한 값인 경우")
        class 유효한_값인_경우 {

            @Test
            @DisplayName("CardType 객체를 생성한다")
            void 객체_생성() {
                // given
                String cardType = "CREDIT";

                // when
                CardType type = new CardType(cardType);

                // then
                assertThat(type.value()).isEqualTo(cardType);
            }
        }
    }
}
