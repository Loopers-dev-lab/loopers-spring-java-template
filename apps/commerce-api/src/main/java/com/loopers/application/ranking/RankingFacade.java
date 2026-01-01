package com.loopers.application.ranking;

import com.loopers.application.product.ProductQueryService;
import com.loopers.application.product.ProductListItem;
import com.loopers.interfaces.api.product.ProductV1Dto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@Component
public class RankingFacade {

  private final RedisTemplate<String, String> redisTemplate;
  private final ProductQueryService productQueryService;
  private final RankingService rankingService;

  @Transactional(readOnly = true)
  public Page<ProductListItem> getProductRankings(Long userId, String date, int size, int page) {
    String rankingKey = "ranking:all:" + date;

    // Redis에서 랭킹 데이터 조회 (페이징 처리)
    long start = (long) (page - 1) * size;
    long end = start + size - 1;

    Set<String> rankedProductIds = redisTemplate.opsForZSet().reverseRange(rankingKey, start, end);

    if (rankedProductIds == null || rankedProductIds.isEmpty()) {
      return new PageImpl<>(List.of(), PageRequest.of(page - 1, size), 0);
    }

    // productIds 리스트 생성
    List<Long> productIds = rankedProductIds.stream()
        .map(Long::parseLong)
        .toList();

    // 상품 정보 일괄 조회 (좋아요 정보 포함)
    List<ProductListItem> productsWithLike = productQueryService.getProductListByProductIds(userId, productIds);
    Long totalCount = rankingService.getTotalRankingCount(date);

    return new PageImpl<>(productsWithLike, PageRequest.of(page - 1, size), totalCount);
  }

  @Transactional(readOnly = true)
  public Page<ProductListItem> getProductRankingsByDate(Long userId, String date, int size, int page) {
    PageRequest pageRequest = PageRequest.of(page - 1, size);

    // date 파라미터를 분석하여 기간별 랭킹 조회
    List<Long> productIds = rankingService.getRankingProductIdsByDate(date, pageRequest);

    if (productIds.isEmpty()) {
      return new PageImpl<>(List.of(), pageRequest, 0);
    }

    // 상품 정보 일괄 조회 (좋아요 정보 포함)
    List<ProductListItem> productsWithLike = productQueryService.getProductListByProductIds(userId, productIds);
    Long totalCount = rankingService.getTotalRankingCountByDate(date);

    return new PageImpl<>(productsWithLike, pageRequest, totalCount);
  }
}
