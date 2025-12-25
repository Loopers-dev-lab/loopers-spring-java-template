package com.loopers.interfaces.api.ranking;

import com.loopers.application.ranking.ProductRankingService;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/rankings")
public class RankingController {

    private final ProductRankingService productRankingService;

    @GetMapping
    public ApiResponse<RankingDto.PageResponse<RankingDto.Response>> getRankings(
            @ModelAttribute RankingDto.SearchRequest request
    ) {
        // 날짜 파싱
        var date = request.toLocalDate();
        
        // Pageable 생성
        Pageable pageable = PageRequest.of(
                request.page() != null ? request.page() : 0,
                request.size() != null ? request.size() : 20
        );
        
        // 랭킹 조회
        Page<ProductRankingService.RankingItem> rankingPage = 
                productRankingService.getTopRankings(date, pageable);
        
        // DTO 변환
        Page<RankingDto.Response> responsePage = rankingPage.map(RankingDto.Response::from);
        
        return ApiResponse.success(RankingDto.PageResponse.from(responsePage));
    }
}



