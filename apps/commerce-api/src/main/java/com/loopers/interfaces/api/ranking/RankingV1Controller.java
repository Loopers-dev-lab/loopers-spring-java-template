package com.loopers.interfaces.api.ranking;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.loopers.application.product.ProductFacade;
import com.loopers.application.product.ProductInfo;
import com.loopers.domain.ranking.RankingPeriod;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.common.PageResponse;
import com.loopers.interfaces.api.product.ProductV1Dtos;
import com.loopers.support.Uris;

import lombok.RequiredArgsConstructor;

/**
 * 랭킹 전용 Controller
 * - 일간/주간/월간 랭킹 API 제공
 * - Java 8 Date API 활용
 */
@RestController
@RequiredArgsConstructor
public class RankingV1Controller implements RankingV1ApiSpec {

    private final ProductFacade productFacade;

    @GetMapping(Uris.Ranking.GET_RANKING)
    @Override
    public ApiResponse<PageResponse<ProductV1Dtos.ProductListResponse>> getRankingProducts(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) LocalDate date
    ) {
        Page<ProductInfo> products = productFacade.getRankingProducts(pageable, date);
        Page<ProductV1Dtos.ProductListResponse> responsePage = products.map(ProductV1Dtos.ProductListResponse::from);
        return ApiResponse.success(PageResponse.from(responsePage));
    }

    @GetMapping(Uris.Ranking.GET_RANKING_BY_PERIOD)
    @Override
    public ApiResponse<PageResponse<ProductV1Dtos.ProductListResponse>> getRankingProductsByPeriod(
            @RequestParam RankingPeriod period,
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) String yearWeek,
            @RequestParam(required = false) String yearMonth
    ) {
        // Java 8 Date API를 활용한 파라미터 검증 및 변환
        String processedYearWeek = processYearWeekParameter(yearWeek, date);
        String processedYearMonth = processYearMonthParameter(yearMonth, date);

        Page<ProductInfo> products = productFacade.getRankingProductsByPeriod(
                period, pageable, date, processedYearWeek, processedYearMonth);
        Page<ProductV1Dtos.ProductListResponse> responsePage = products.map(ProductV1Dtos.ProductListResponse::from);
        return ApiResponse.success(PageResponse.from(responsePage));
    }

    /**
     * yearWeek 파라미터 처리
     * - 파라미터가 없으면 현재 주차로 설정
     * - Java 8 WeekFields 활용
     */
    private String processYearWeekParameter(String yearWeek, LocalDate date) {
        if (yearWeek != null && !yearWeek.trim().isEmpty()) {
            return yearWeek;
        }

        // date가 있으면 해당 날짜의 주차, 없으면 현재 주차
        LocalDate targetDate = date != null ? date : LocalDate.now();

        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        int year = targetDate.getYear();
        int week = targetDate.get(weekFields.weekOfYear());

        return String.format("%d-W%02d", year, week);
    }

    /**
     * yearMonth 파라미터 처리
     * - 파라미터가 없으면 현재 월로 설정
     * - Java 8 YearMonth 활용
     */
    private String processYearMonthParameter(String yearMonth, LocalDate date) {
        if (yearMonth != null && !yearMonth.trim().isEmpty()) {
            return yearMonth;
        }

        // date가 있으면 해당 날짜의 월, 없으면 현재 월
        LocalDate targetDate = date != null ? date : LocalDate.now();
        YearMonth ym = YearMonth.from(targetDate);

        return ym.format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }
}
