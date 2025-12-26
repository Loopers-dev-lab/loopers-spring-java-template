package com.loopers.interfaces.api.ranking;

import com.loopers.application.ranking.RankingItemResult;
import com.loopers.application.ranking.RankingResult;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public class RankingDto {

  private RankingDto() {}

  public record RankingResponse(
      List<RankingItemResponse> rankings,
      int page,
      int size,
      LocalDate date
  ) {
    public static RankingResponse from(RankingResult result) {
      Objects.requireNonNull(result, "result는 null일 수 없습니다.");
      List<RankingItemResponse> rankings = result.rankings().stream()
          .map(RankingItemResponse::from)
          .toList();
      return new RankingResponse(rankings, result.page(), result.size(), result.date());
    }
  }

  public record RankingItemResponse(
      int rank,
      double score,
      Long productId,
      String productName,
      Long price,
      String brandName,
      boolean liked
  ) {
    public static RankingItemResponse from(RankingItemResult item) {
      Objects.requireNonNull(item, "item은 null일 수 없습니다.");
      return new RankingItemResponse(
          item.rank(),
          item.score(),
          item.productId(),
          item.productName(),
          item.price(),
          item.brandName(),
          item.liked()
      );
    }
  }
}
