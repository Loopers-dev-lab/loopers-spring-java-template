package com.loopers.interfaces.api.ranking;

import com.loopers.application.ranking.ProductRankingService;
import com.loopers.interfaces.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/rankings")
public class RankingController {

    private final ProductRankingService productRankingService;

    /**
     * 랭킹 조회 (Query Parameter 방식)
     * GET /api/v1/rankings?datetime=20251201000000&period=hourly&size=20&page=1
     * 
     * @param request 조회 요청 (datetime, period, page, size)
     * @return 랭킹 응답
     */
    @GetMapping
    public ApiResponse<RankingDto.PageResponse<RankingDto.Response>> getRankings(
            @Valid @ModelAttribute RankingDto.SearchRequest request
    ) {
        // 필수 파라미터 검증
        if (request.datetime() == null || request.datetime().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "datetime parameter is required (format: yyyyMMddHHmmss)");
        }
        
        if (request.period() == null || request.period().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "period parameter is required (hourly, daily, weekly, monthly)");
        }
        
        // period 검증
        if (!isValidPeriod(request.period())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Invalid period: " + request.period() + ". Valid periods are: hourly, daily, weekly, monthly");
        }
        
        // Pageable 생성 (기본값: page=0, size=10)
        Pageable pageable = PageRequest.of(
                request.page() != null ? request.page() : 0,
                request.size() != null ? request.size() : 10
        );
        
        // 특정 datetime의 랭킹 조회
        Page<ProductRankingService.RankingItem> rankingPage = productRankingService
            .getTopRankingsByDatetime(request.datetime(), request.period(), pageable);
        
        // snapshotTime 추출 (첫 번째 아이템에서, null이 아닌 경우)
        LocalDateTime snapshotTime = null;
        if (!rankingPage.getContent().isEmpty()) {
            LocalDateTime firstSnapshotTime = rankingPage.getContent().get(0).snapshotTime();
            if (firstSnapshotTime != null) {
                snapshotTime = firstSnapshotTime;
            }
        }
        
        Page<RankingDto.Response> responsePage = rankingPage.map(RankingDto.Response::from);
        
        return ApiResponse.success(RankingDto.PageResponse.from(
            request.period().toUpperCase(), snapshotTime, responsePage));
    }

    /**
     * 랭킹 조회 (Path Variable 방식) - 하위 호환성 유지
     * 
     * @param type 랭킹 타입 (hourly, daily, weekly, monthly)
     * @param request 조회 요청 (page, size)
     * @return 랭킹 응답
     */
    @GetMapping("/{type}")
    public ApiResponse<RankingDto.PageResponse<RankingDto.Response>> getRankingsByType(
            @PathVariable String type,
            @Valid @ModelAttribute RankingDto.SearchRequest request
    ) {
        // 타입 검증
        if (!isValidType(type)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Invalid ranking type: " + type + ". Valid types are: hourly, daily, weekly, monthly");
        }
        
        // Pageable 생성 (기본값: page=0, size=10)
        Pageable pageable = PageRequest.of(
                request.page() != null ? request.page() : 0,
                request.size() != null ? request.size() : 10
        );
        
        // 타입별 랭킹 조회
        RankingResult result = switch (type.toLowerCase()) {
            case "hourly" -> new RankingResult(
                productRankingService.getTopRankingsHourly(
                    LocalDateTime.now().withMinute(0).withSecond(0).withNano(0), 
                    pageable
                ),
                "HOURLY"
            );
            case "daily" -> new RankingResult(
                productRankingService.getTopRankingsDaily(
                    LocalDate.now(), 
                    pageable
                ),
                "DAILY"
            );
            case "weekly" -> new RankingResult(
                productRankingService.getTopRankingsWeekly(pageable),
                "WEEKLY"
            );
            case "monthly" -> new RankingResult(
                productRankingService.getTopRankingsMonthly(pageable),
                "MONTHLY"
            );
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Invalid ranking type: " + type);
        };
        
        Page<ProductRankingService.RankingItem> rankingPage = result.rankingPage();
        String rankingType = result.rankingType();
        
        // snapshotTime 추출 (첫 번째 아이템에서, null이 아닌 경우)
        LocalDateTime snapshotTime = null;
        if (!rankingPage.getContent().isEmpty()) {
            LocalDateTime firstSnapshotTime = rankingPage.getContent().get(0).snapshotTime();
            if (firstSnapshotTime != null) {
                snapshotTime = firstSnapshotTime;
            }
        }
        
        Page<RankingDto.Response> responsePage = rankingPage.map(RankingDto.Response::from);
        
        return ApiResponse.success(RankingDto.PageResponse.from(rankingType, snapshotTime, responsePage));
    }

    /**
     * period 검증
     */
    private boolean isValidPeriod(String period) {
        if (period == null) {
            return false;
        }
        String lowerPeriod = period.toLowerCase();
        return switch (lowerPeriod) {
            case "hourly", "daily", "weekly", "monthly" -> true;
            default -> false;
        };
    }

    /**
     * 타입 검증 (Path Variable용)
     */
    private boolean isValidType(String type) {
        if (type == null) {
            return false;
        }
        String lowerType = type.toLowerCase();
        return switch (lowerType) {
            case "hourly", "daily", "weekly", "monthly" -> true;
            default -> false;
        };
    }

    /**
     * 랭킹 조회 결과 임시 저장
     */
    private record RankingResult(
        Page<ProductRankingService.RankingItem> rankingPage,
        String rankingType
    ) {}
}



