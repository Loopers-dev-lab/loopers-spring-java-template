package com.loopers.application.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.CatalogEventHandler;
import com.loopers.domain.metrics.MetricDateConverter;
import com.loopers.domain.metrics.ProductMetrics;
import com.loopers.domain.metrics.ProductMetricsId;
import com.loopers.infrastructure.metrics.ProductMetricsJpaRepository;
import com.loopers.domain.event.EventType;
import com.loopers.infrastructure.ranking.RankingRedisProperties;
import com.loopers.support.test.IntegrationTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("ProductMetrics 집계 통합 테스트")
class ProductMetricsIntegrationTest extends IntegrationTestSupport {

  @Autowired
  private CatalogEventHandler catalogEventHandler;

  @Autowired
  private ProductMetricsJpaRepository productMetricsJpaRepository;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private RankingRedisProperties rankingProperties;

  @Nested
  @DisplayName("sales_count 집계")
  class SalesCountAggregation {

    @Test
    @DisplayName("PRODUCT_SOLD 이벤트 처리 시 sales_count가 quantity만큼 증가한다")
    void shouldIncreaseSalesCount_whenProductSoldEventReceived() throws Exception {
      Long productId = 100L;
      int quantity = 3;
      String eventId = UUID.randomUUID().toString();
      long occurredAt = System.currentTimeMillis();
      JsonNode payload = objectMapper.readTree("{\"quantity\":" + quantity + ",\"orderId\":1}");

      catalogEventHandler.handle(
          eventId, EventType.PRODUCT_SOLD.getCode(), String.valueOf(productId), occurredAt, payload);

      Integer metricDate = MetricDateConverter.toMetricDate(occurredAt, rankingProperties.getTimezone());
      ProductMetrics metrics = productMetricsJpaRepository.findById(ProductMetricsId.of(productId, metricDate)).orElseThrow();
      assertThat(metrics.getSalesCount()).isEqualTo(quantity);
    }

    @Test
    @DisplayName("동일 상품에 여러 번 판매 이벤트 발생 시 sales_count가 누적된다")
    void shouldAccumulateSalesCount_whenMultipleProductSoldEvents() throws Exception {
      Long productId = 101L;
      long now = System.currentTimeMillis();

      catalogEventHandler.handle(
          UUID.randomUUID().toString(),
          EventType.PRODUCT_SOLD.getCode(),
          String.valueOf(productId),
          now,
          objectMapper.readTree("{\"quantity\":2,\"orderId\":1}"));

      catalogEventHandler.handle(
          UUID.randomUUID().toString(),
          EventType.PRODUCT_SOLD.getCode(),
          String.valueOf(productId),
          now + 1,
          objectMapper.readTree("{\"quantity\":5,\"orderId\":2}"));

      Integer metricDate = MetricDateConverter.toMetricDate(now, rankingProperties.getTimezone());
      ProductMetrics metrics = productMetricsJpaRepository.findById(ProductMetricsId.of(productId, metricDate)).orElseThrow();
      assertThat(metrics.getSalesCount()).isEqualTo(7);
    }

    @Test
    @DisplayName("quantity 필드가 없으면 예외가 발생한다")
    void shouldThrowException_whenQuantityMissing() throws Exception {
      Long productId = 102L;
      String eventId = UUID.randomUUID().toString();
      JsonNode payloadWithoutQuantity = objectMapper.readTree("{\"orderId\":1}");

      assertThatThrownBy(
              () ->
                  catalogEventHandler.handle(
                      eventId,
                      EventType.PRODUCT_SOLD.getCode(),
                      String.valueOf(productId),
                      System.currentTimeMillis(),
                      payloadWithoutQuantity))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("quantity");
    }
  }

  @Nested
  @DisplayName("like_count 집계")
  class LikeCountAggregation {

    @Test
    @DisplayName("PRODUCT_LIKED 이벤트 처리 시 like_count가 1 증가한다")
    void shouldIncreaseLikeCount_whenProductLikedEventReceived() throws Exception {
      Long productId = 200L;
      String eventId = UUID.randomUUID().toString();
      long occurredAt = System.currentTimeMillis();
      JsonNode emptyPayload = objectMapper.readTree("{}");

      catalogEventHandler.handle(
          eventId,
          EventType.PRODUCT_LIKED.getCode(),
          String.valueOf(productId),
          occurredAt,
          emptyPayload);

      Integer metricDate = MetricDateConverter.toMetricDate(occurredAt, rankingProperties.getTimezone());
      ProductMetrics metrics = productMetricsJpaRepository.findById(ProductMetricsId.of(productId, metricDate)).orElseThrow();
      assertThat(metrics.getLikeCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("PRODUCT_UNLIKED 이벤트 처리 시 like_count가 1 감소한다")
    void shouldDecreaseLikeCount_whenProductUnlikedEventReceived() throws Exception {
      Long productId = 201L;
      long now = System.currentTimeMillis();
      JsonNode emptyPayload = objectMapper.readTree("{}");

      // 먼저 좋아요 2회
      catalogEventHandler.handle(
          UUID.randomUUID().toString(),
          EventType.PRODUCT_LIKED.getCode(),
          String.valueOf(productId),
          now,
          emptyPayload);
      catalogEventHandler.handle(
          UUID.randomUUID().toString(),
          EventType.PRODUCT_LIKED.getCode(),
          String.valueOf(productId),
          now + 1,
          emptyPayload);

      // 좋아요 취소 1회
      catalogEventHandler.handle(
          UUID.randomUUID().toString(),
          EventType.PRODUCT_UNLIKED.getCode(),
          String.valueOf(productId),
          now + 2,
          emptyPayload);

      Integer metricDate = MetricDateConverter.toMetricDate(now, rankingProperties.getTimezone());
      ProductMetrics metrics = productMetricsJpaRepository.findById(ProductMetricsId.of(productId, metricDate)).orElseThrow();
      assertThat(metrics.getLikeCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("like_count는 0 미만으로 내려가지 않는다")
    void shouldNotGoBelowZero_whenUnlikedMoreThanLiked() throws Exception {
      Long productId = 202L;
      long occurredAt = System.currentTimeMillis();
      JsonNode emptyPayload = objectMapper.readTree("{}");

      // 좋아요 없이 취소 시도
      catalogEventHandler.handle(
          UUID.randomUUID().toString(),
          EventType.PRODUCT_UNLIKED.getCode(),
          String.valueOf(productId),
          occurredAt,
          emptyPayload);

      Integer metricDate = MetricDateConverter.toMetricDate(occurredAt, rankingProperties.getTimezone());
      ProductMetrics metrics = productMetricsJpaRepository.findById(ProductMetricsId.of(productId, metricDate)).orElseThrow();
      assertThat(metrics.getLikeCount()).isZero();
    }
  }

  @Nested
  @DisplayName("view_count 집계")
  class ViewCountAggregation {

    @Test
    @DisplayName("PRODUCT_VIEWED 이벤트 처리 시 view_count가 1 증가한다")
    void shouldIncreaseViewCount_whenProductViewedEventReceived() throws Exception {
      Long productId = 300L;
      String eventId = UUID.randomUUID().toString();
      long occurredAt = System.currentTimeMillis();
      JsonNode emptyPayload = objectMapper.readTree("{}");

      catalogEventHandler.handle(
          eventId,
          EventType.PRODUCT_VIEWED.getCode(),
          String.valueOf(productId),
          occurredAt,
          emptyPayload);

      Integer metricDate = MetricDateConverter.toMetricDate(occurredAt, rankingProperties.getTimezone());
      ProductMetrics metrics = productMetricsJpaRepository.findById(ProductMetricsId.of(productId, metricDate)).orElseThrow();
      assertThat(metrics.getViewCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("동일 상품에 여러 번 조회 이벤트 발생 시 view_count가 누적된다")
    void shouldAccumulateViewCount_whenMultipleProductViewedEvents() throws Exception {
      Long productId = 301L;
      long now = System.currentTimeMillis();
      JsonNode emptyPayload = objectMapper.readTree("{}");

      catalogEventHandler.handle(
          UUID.randomUUID().toString(),
          EventType.PRODUCT_VIEWED.getCode(),
          String.valueOf(productId),
          now,
          emptyPayload);

      catalogEventHandler.handle(
          UUID.randomUUID().toString(),
          EventType.PRODUCT_VIEWED.getCode(),
          String.valueOf(productId),
          now + 1,
          emptyPayload);

      catalogEventHandler.handle(
          UUID.randomUUID().toString(),
          EventType.PRODUCT_VIEWED.getCode(),
          String.valueOf(productId),
          now + 2,
          emptyPayload);

      Integer metricDate = MetricDateConverter.toMetricDate(now, rankingProperties.getTimezone());
      ProductMetrics metrics = productMetricsJpaRepository.findById(ProductMetricsId.of(productId, metricDate)).orElseThrow();
      assertThat(metrics.getViewCount()).isEqualTo(3);
    }
  }
}
