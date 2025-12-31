package com.loopers.application.ranking;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductLikeCountService;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.ranking.RankingService;
import com.loopers.interfaces.api.ranking.RankingV1Dto.RankingProductResponse;
import com.loopers.ranking.streamer.RankingInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingFacade {
    private final RankingService rankingService;
    private final ProductService productService;
    private final ProductLikeCountService productLikeCountService;

    public Page<RankingProductResponse> getRankings(LocalDate rankingDate, Pageable pageable) {

        // 1. Redis ZSET에서 랭킹 상품 ID와 점수를 조회
        List<RankingInfo> rankedProducts = rankingService.getRankings(rankingDate, pageable);
        long totalCount = rankingService.getTotalCount(rankingDate);

        // 2. 랭킹 상품 ID들을 추출
        List<Long> productIds = rankedProducts.stream()
                .map(RankingInfo::id)
                .toList();

        // 3. ProductService 통해 상품 상세 정보 조회 (상품 ID 순서 유지를 위해 Map 사용)
        Map<Long, Product> productInfos = productService.getProductsMapByIds(productIds);

        // 4. 조회된 상품 정보와 랭킹 데이터를 결합하여 응답 DTO 생성
        List<RankingProductResponse> rankingResponses = rankedProducts.stream()
                .map(ranking -> {
                    Product product = productInfos.get(ranking.id());

                    if (product == null) {
                        log.warn("상품 정보를 찾을 수 없습니다. 상품ID : {}", ranking.id());
                        return null;
                    }

                    // 좋아요 수를 ProductLikeCountService에서 조회 (없으면 0)
                    int likeCount = productLikeCountService.findById(product.getId())
                            .map(pc -> pc.getLikeCount())
                            .orElse(0);

                    return new RankingProductResponse(
                            product.getId(),
                            product.getName(),
                            product.getBrandId(),
                            product.getPrice().getAmount(),
                            likeCount,
                            ranking.score(),
                            ranking.rank(),
                            LocalDateTime.now()     // createdAt?
                    );
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Page<RankingProductResponse> responsePage = new PageImpl<>(
                rankingResponses,
                pageable,
                totalCount
        );

        return responsePage;
    };
}
