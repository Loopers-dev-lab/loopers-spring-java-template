package com.loopers.domain.common;

import com.loopers.domain.common.vo.Money;
import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Money 값 객체")
class MoneyTest {

    @DisplayName("생성")
    @Nested
    class Creation {
        
        @DisplayName("유효한 BigDecimal 금액으로 Money를 생성할 수 있다")
        @Test
        void should_create_money_with_valid_amount() {
            Money money = Money.of(BigDecimal.valueOf(1000));
            
            assertThat(money.getAmount()).isEqualTo(BigDecimal.valueOf(1000));
        }

        @DisplayName("long 타입 값으로 Money를 생성할 수 있다")
        @Test
        void should_create_money_from_long_value() {
            Money money = Money.of(1000L);
            
            assertThat(money.getAmount()).isEqualTo(BigDecimal.valueOf(1000));
        }

        @DisplayName("0원 Money를 생성할 수 있다")
        @Test
        void should_create_zero_money() {
            Money zero = Money.zero();
            
            assertThat(zero.getAmount()).isEqualTo(BigDecimal.ZERO);
        }
    }

    @DisplayName("검증")
    @Nested
    class Validation {
        
        @DisplayName("null 금액으로 생성 시 예외가 발생한다")
        @Test
        void should_throw_exception_when_amount_is_null() {
            assertThatThrownBy(() -> Money.of((BigDecimal) null))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("금액은 null일 수 없습니다");
        }

        @DisplayName("음수 금액으로 생성 시 예외가 발생한다")
        @Test
        void should_throw_exception_when_amount_is_negative() {
            assertThatThrownBy(() -> Money.of(BigDecimal.valueOf(-1)))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("금액은 0 이상이어야 합니다");
        }
    }

    @DisplayName("산술 연산")
    @Nested
    class ArithmeticOperations {
        
        @DisplayName("두 Money를 더할 수 있다")
        @Test
        void should_add_money_correctly() {
            Money money1 = Money.of(1000);
            Money money2 = Money.of(2000);
            
            Money result = money1.plus(money2);
            
            assertThat(result.getAmount()).isEqualTo(BigDecimal.valueOf(3000));
            // 원본은 불변
            assertThat(money1.getAmount()).isEqualTo(BigDecimal.valueOf(1000));
            assertThat(money2.getAmount()).isEqualTo(BigDecimal.valueOf(2000));
        }

        @DisplayName("두 Money를 뺄 수 있다")
        @Test
        void should_subtract_money_correctly() {
            Money money1 = Money.of(3000);
            Money money2 = Money.of(1000);
            
            Money result = money1.minus(money2);
            
            assertThat(result.getAmount()).isEqualTo(BigDecimal.valueOf(2000));
        }

        @DisplayName("빼기 연산 시 음수 결과를 허용한다")
        @Test
        void should_allow_negative_result_when_subtracting() {
            Money money1 = Money.of(1000);
            Money money2 = Money.of(2000);
            
            // 빼기는 음수 결과를 허용 (비즈니스 로직에서 검증)
            Money result = money1.minus(money2);
            
            assertThat(result.getAmount()).isEqualTo(BigDecimal.valueOf(-1000));
        }

        @DisplayName("Money에 정수를 곱할 수 있다")
        @Test
        void should_multiply_money_correctly() {
            Money money = Money.of(1000);
            
            Money result = money.multiply(3);
            
            assertThat(result.getAmount()).isEqualTo(BigDecimal.valueOf(3000));
            assertThat(money.getAmount()).isEqualTo(BigDecimal.valueOf(1000)); // 원본 불변
        }
    }

    @DisplayName("비교 연산")
    @Nested
    class ComparisonOperations {
        
        @DisplayName("Money 금액을 비교할 수 있다")
        @Test
        void should_compare_money_amounts() {
            Money money1 = Money.of(1000);
            Money money2 = Money.of(2000);
            Money money3 = Money.of(1000);
            
            assertThat(money2.isGreaterThanOrEqual(money1)).isTrue();
            assertThat(money1.isGreaterThanOrEqual(money3)).isTrue();
            assertThat(money1.isGreaterThanOrEqual(money2)).isFalse();
            
            assertThat(money1.isLessThan(money2)).isTrue();
            assertThat(money1.isLessThan(money3)).isFalse();
            assertThat(money2.isLessThan(money1)).isFalse();
        }
    }

    @DisplayName("객체 동등성")
    @Nested
    class ObjectEquality {
        
        @DisplayName("같은 금액의 Money는 동등하다")
        @Test
        void should_check_money_equality() {
            Money money1 = Money.of(1000);
            Money money2 = Money.of(1000);
            Money money3 = Money.of(2000);
            
            assertThat(money1).isEqualTo(money2);
            assertThat(money1).isNotEqualTo(money3);
            assertThat(money1.hashCode()).isEqualTo(money2.hashCode());
        }

        @DisplayName("toString()에 금액이 포함된다")
        @Test
        void should_have_proper_string_representation() {
            Money money = Money.of(1000);
            
            assertThat(money.toString()).contains("1000");
        }
    }

    @DisplayName("복합 연산")
    @Nested
    class ComplexOperations {
        
        @DisplayName("연산을 체이닝할 수 있다")
        @Test
        void should_support_chained_operations() {
            Money money = Money.of(1000);
            
            Money result = money
                    .plus(Money.of(500))
                    .multiply(2)
                    .minus(Money.of(1000));
            
            assertThat(result.getAmount()).isEqualTo(BigDecimal.valueOf(2000));
            // 1000 + 500 = 1500
            // 1500 * 2 = 3000  
            // 3000 - 1000 = 2000
        }

        @DisplayName("0원과의 연산을 올바르게 처리한다")
        @Test
        void should_handle_zero_money_operations() {
            Money zero = Money.zero();
            Money money = Money.of(1000);
            
            assertThat(zero.plus(money)).isEqualTo(money);
            assertThat(money.plus(zero)).isEqualTo(money);
            assertThat(money.minus(zero)).isEqualTo(money);
            assertThat(zero.multiply(5)).isEqualTo(zero);
        }
    }
}