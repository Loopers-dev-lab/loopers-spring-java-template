package com.loopers.interfaces.api.ranking;

import com.loopers.application.ranking.ProductRankingService;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/rankings")
public class RankingController {

    private final ProductRankingService productRankingService;

    /**
     * 시간 단위 랭킹 조회
     */
    @GetMapping("/hourly")
    public ApiResponse<RankingDto.PageResponse<RankingDto.Response>> getHourlyRankings(
            @ModelAttribute RankingDto.HourlySearchRequest request
    ) {
        Pageable pageable = PageRequest.of(
                request.page() != null ? request.page() : 0,
                request.size() != null ? request.size() : 20
        );
        
        LocalDateTime hour = request.toLocalDateTime();
        Page<ProductRankingService.RankingItem> rankingPage = 
                productRankingService.getTopRankingsHourly(hour, pageable);
        
        Page<RankingDto.Response> responsePage = rankingPage.map(RankingDto.Response::from);
        
        return ApiResponse.success(RankingDto.PageResponse.from(responsePage));
    }

    /**
     * 일 단위 랭킹 조회
     */
    @GetMapping("/daily")
    public ApiResponse<RankingDto.PageResponse<RankingDto.Response>> getDailyRankings(
            @ModelAttribute RankingDto.DailySearchRequest request
    ) {
        Pageable pageable = PageRequest.of(
                request.page() != null ? request.page() : 0,
                request.size() != null ? request.size() : 20
        );
        
        var date = request.toLocalDate();
        Page<ProductRankingService.RankingItem> rankingPage = 
                productRankingService.getTopRankingsDaily(date, pageable);
        
        Page<RankingDto.Response> responsePage = rankingPage.map(RankingDto.Response::from);
        
        return ApiResponse.success(RankingDto.PageResponse.from(responsePage));
    }

    /**
     * 기본 랭킹 조회 (일 단위, 하위 호환성)
     */
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
        
        // 랭킹 조회 (일 단위로 위임)
        Page<ProductRankingService.RankingItem> rankingPage = 
                productRankingService.getTopRankingsDaily(date, pageable);
        
        // DTO 변환
        Page<RankingDto.Response> responsePage = rankingPage.map(RankingDto.Response::from);
        
        return ApiResponse.success(RankingDto.PageResponse.from(responsePage));
    }
}



