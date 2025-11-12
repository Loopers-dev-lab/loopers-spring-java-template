package com.loopers.domain.product;

import com.loopers.domain.money.Money;
import com.loopers.domain.stock.Stock;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Product 도메인 테스트")
class ProductTest {

  @DisplayName("Product를 생성할 때")
  @Nested
  class Create {

    @DisplayName("올바른 정보로 생성하면 성공한다")
    @Test
    void shouldCreate_whenValid() {
      String name = "나이키 에어맥스";
      Money price = Money.of(150000L);
      String description = "편안한 운동화";
      Stock stock = Stock.of(100L);
      Long brandId = 1L;

      Product product = Product.of(name, price, description, stock, brandId);

      assertThat(product).extracting("name", "description", "brandId")
          .containsExactly(name, description, brandId);
      assertThat(product).extracting("price").isEqualTo(price);
      assertThat(product).extracting("stock").isEqualTo(stock);
      assertThat(product).extracting("likeCount").isEqualTo(0L);
    }
  }

  @DisplayName("name 검증")
  @Nested
  class ValidateName {

    @DisplayName("100자 이내면 정상 생성된다")
    @Test
    void shouldCreate_whenValidLength() {
      String maxLengthName = "a".repeat(100);
      Money price = Money.of(10000L);
      Stock stock = Stock.of(10L);

      Product product = Product.of(maxLengthName, price, "설명", stock, 1L);

      assertThat(product).extracting("name").isEqualTo(maxLengthName);
    }

    @DisplayName("null 또는 빈 문자열이면 예외가 발생한다")
    @ParameterizedTest
    @NullAndEmptySource
    void shouldThrowException_whenNullOrEmpty(String invalidName) {
      Money price = Money.of(10000L);
      Stock stock = Stock.of(10L);

      assertThatThrownBy(() -> Product.of(invalidName, price, "설명", stock, 1L))
          .isInstanceOf(CoreException.class)
          .hasMessage("상품명은 비어있을 수 없습니다.")
          .extracting("errorType").isEqualTo(ErrorType.INVALID_PRODUCT_NAME_EMPTY);
    }

    @DisplayName("100자를 초과하면 예외가 발생한다")
    @Test
    void shouldThrowException_whenTooLong() {
      String tooLongName = "a".repeat(101);
      Money price = Money.of(10000L);
      Stock stock = Stock.of(10L);

      assertThatThrownBy(() -> Product.of(tooLongName, price, "설명", stock, 1L))
          .isInstanceOf(CoreException.class)
          .hasMessage("상품명은 100자 이내여야 합니다.")
          .extracting("errorType").isEqualTo(ErrorType.INVALID_PRODUCT_NAME_LENGTH);
    }
  }

  @DisplayName("price 검증")
  @Nested
  class ValidatePrice {

    @DisplayName("null이면 예외가 발생한다")
    @Test
    void shouldThrowException_whenNull() {
      Stock stock = Stock.of(10L);

      assertThatThrownBy(() -> Product.of("상품명", null, "설명", stock, 1L))
          .isInstanceOf(CoreException.class)
          .hasMessage("가격은 비어있을 수 없습니다.")
          .extracting("errorType").isEqualTo(ErrorType.INVALID_PRODUCT_PRICE_EMPTY);
    }
  }

  @DisplayName("stock 검증")
  @Nested
  class ValidateStock {

    @DisplayName("null이면 예외가 발생한다")
    @Test
    void shouldThrowException_whenNull() {
      Money price = Money.of(10000L);

      assertThatThrownBy(() -> Product.of("상품명", price, "설명", null, 1L))
          .isInstanceOf(CoreException.class)
          .hasMessage("재고는 비어있을 수 없습니다.")
          .extracting("errorType").isEqualTo(ErrorType.INVALID_PRODUCT_STOCK_EMPTY);
    }
  }

  @DisplayName("brandId 검증")
  @Nested
  class ValidateBrandId {

    @DisplayName("null이면 예외가 발생한다")
    @Test
    void shouldThrowException_whenNull() {
      Money price = Money.of(10000L);
      Stock stock = Stock.of(10L);

      assertThatThrownBy(() -> Product.of("상품명", price, "설명", stock, null))
          .isInstanceOf(CoreException.class)
          .hasMessage("브랜드는 비어있을 수 없습니다.")
          .extracting("errorType").isEqualTo(ErrorType.INVALID_PRODUCT_BRAND_EMPTY);
    }
  }

  @DisplayName("비즈니스 로직")
  @Nested
  class BusinessLogic {

    @DisplayName("재고가 충분하면 차감에 성공한다")
    @Test
    void shouldDecreaseStock_whenSufficient() {
      Product product = Product.of("상품명", Money.of(10000L), "설명", Stock.of(100L), 1L);

      product.decreaseStock(30L);

      assertThat(product).extracting("stock").isEqualTo(Stock.of(70L));
    }

    @DisplayName("재고가 부족하면 예외가 발생한다")
    @Test
    void shouldThrowException_whenInsufficientStock() {
      Product product = Product.of("상품명", Money.of(10000L), "설명", Stock.of(10L), 1L);

      assertThatThrownBy(() -> product.decreaseStock(20L))
          .isInstanceOf(CoreException.class)
          .hasMessage("재고가 부족합니다.")
          .extracting("errorType").isEqualTo(ErrorType.INSUFFICIENT_STOCK);
    }

    @DisplayName("재고가 있으면 구매 가능하다")
    @Test
    void shouldReturnTrue_whenAvailable() {
      Product product = Product.of("상품명", Money.of(10000L), "설명", Stock.of(1L), 1L);

      boolean available = product.isAvailable();

      assertThat(available).isTrue();
    }

    @DisplayName("재고가 0이면 구매 불가능하다")
    @Test
    void shouldReturnFalse_whenNotAvailable() {
      Product product = Product.of("상품명", Money.of(10000L), "설명", Stock.zero(), 1L);

      boolean available = product.isAvailable();

      assertThat(available).isFalse();
    }

    @DisplayName("좋아요 수를 증가시킨다")
    @Test
    void shouldIncreaseLikeCount() {
      Product product = Product.of("상품명", Money.of(10000L), "설명", Stock.of(10L), 1L);

      product.increaseLikeCount();

      assertThat(product).extracting("likeCount").isEqualTo(1L);
    }

    @DisplayName("좋아요 수를 감소시킨다")
    @Test
    void shouldDecreaseLikeCount() {
      Product product = Product.of("상품명", Money.of(10000L), "설명", Stock.of(10L), 1L);
      product.increaseLikeCount();
      product.increaseLikeCount();

      product.decreaseLikeCount();

      assertThat(product).extracting("likeCount").isEqualTo(1L);
    }

    @DisplayName("좋아요 수가 0일 때 감소시켜도 음수가 되지 않는다")
    @Test
    void shouldNotDecreaseLikeCount_whenZero() {
      Product product = Product.of("상품명", Money.of(10000L), "설명", Stock.of(10L), 1L);

      product.decreaseLikeCount();

      assertThat(product).extracting("likeCount").isEqualTo(0L);
    }
  }
}
