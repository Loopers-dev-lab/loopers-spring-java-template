package com.loopers.application.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductOutOfStockStrategy 단위 테스트")
class ProductOutOfStockStrategyTest {

  @Mock
  private StringRedisTemplate redisTemplate;

  @InjectMocks
  private ProductOutOfStockStrategy strategy;

  @Nested
  @DisplayName("supports")
  class Supports {

    @Test
    @DisplayName("product_out_of_stock 이벤트 타입을 지원한다")
    void shouldReturnTrue_whenEventTypeIsProductOutOfStock() {
      // when
      boolean result = strategy.supports("product_out_of_stock");

      // then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("다른 이벤트 타입은 지원하지 않는다")
    void shouldReturnFalse_whenEventTypeIsNotProductOutOfStock() {
      // when & then
      assertThat(strategy.supports("product_liked")).isFalse();
      assertThat(strategy.supports("product_unliked")).isFalse();
      assertThat(strategy.supports("unknown_event")).isFalse();
    }
  }

  @Nested
  @DisplayName("handle")
  class Handle {

    @Test
    @DisplayName("상품 캐시를 삭제한다")
    void shouldDeleteProductCache() {
      // given
      Long productId = 123L;
      Long occurredAt = System.currentTimeMillis();
      ObjectNode emptyPayload = new ObjectMapper().createObjectNode();

      // when
      strategy.handle(productId, occurredAt, emptyPayload);

      // then
      then(redisTemplate).should(times(1)).delete("product:v1:123");
    }

  }
}
