package com.loopers.interfaces.api.ranking;

import com.loopers.application.ranking.RankingFacade;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.product.ProductV1Dto;
import com.loopers.interfaces.api.ranking.RankingV1Dto.RankingProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/rankings")
public class RankingV1Controller implements RankingV1ApiSpec {

    private final RankingFacade rankingFacade;

    @Override
    public ApiResponse<ProductV1Dto.PageResponse<RankingProductResponse>> getRankings(
            Pageable pageable
    ) {
        LocalDate rankingDate = LocalDate.now();

        Page<RankingProductResponse> page = rankingFacade.getRankings(rankingDate, pageable);

        return ApiResponse.success(ProductV1Dto.PageResponse.from(page));
    }
}
