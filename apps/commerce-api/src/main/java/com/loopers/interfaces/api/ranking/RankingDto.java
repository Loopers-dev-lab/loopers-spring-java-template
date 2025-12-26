package com.loopers.interfaces.api.ranking;

import com.loopers.application.ranking.ProductRankingService;
import com.loopers.domain.product.view.ProductView;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class RankingDto {

    /**
     * 랭킹 조회 요청 (기본, 일 단위)
     */
    public record SearchRequest(
        String date,  // yyyyMMdd 형식
        Integer page,
        Integer size
    ) {
        public LocalDate toLocalDate() {
            if (date == null || date.isEmpty()) {
                return LocalDate.now();
            }
            return LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyyMMdd"));
        }
    }

    /**
     * 시간 단위 랭킹 조회 요청
     */
    public record HourlySearchRequest(
        String hour,  // yyyyMMddHH 형식
        Integer page,
        Integer size
    ) {
        public LocalDateTime toLocalDateTime() {
            if (hour == null || hour.isEmpty()) {
                return LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
            }
            // yyyyMMddHH 형식 파싱
            int year = Integer.parseInt(hour.substring(0, 4));
            int month = Integer.parseInt(hour.substring(4, 6));
            int day = Integer.parseInt(hour.substring(6, 8));
            int hourOfDay = Integer.parseInt(hour.substring(8, 10));
            return LocalDateTime.of(year, month, day, hourOfDay, 0, 0);
        }
    }

    /**
     * 일 단위 랭킹 조회 요청
     */
    public record DailySearchRequest(
        String date,  // yyyyMMdd 형식
        Integer page,
        Integer size
    ) {
        public LocalDate toLocalDate() {
            if (date == null || date.isEmpty()) {
                return LocalDate.now();
            }
            return LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyyMMdd"));
        }
    }

    /**
     * 월 단위 랭킹 조회 요청
     */
    public record MonthlySearchRequest(
        String month,  // yyyyMM 형식
        Integer page,
        Integer size
    ) {
        public YearMonth toYearMonth() {
            if (month == null || month.isEmpty()) {
                return YearMonth.now();
            }
            return YearMonth.parse(month, DateTimeFormatter.ofPattern("yyyyMM"));
        }
    }

    /**
     * 연 단위 랭킹 조회 요청
     */
    public record YearlySearchRequest(
        Integer year,
        Integer page,
        Integer size
    ) {
        public Integer toYear() {
            if (year == null) {
                return LocalDate.now().getYear();
            }
            return year;
        }
    }

    /**
     * 랭킹 응답
     */
    public record Response(
        Long rank,
        Long productId,
        ProductInfo productInfo
    ) {
        public static Response from(ProductRankingService.RankingItem item) {
            return new Response(
                item.rank(),
                item.productId(),
                ProductInfo.from(item.productView())
            );
        }
    }

    /**
     * 상품 정보
     */
    public record ProductInfo(
        Long id,
        String name,
        Long price,
        Long likeCount,
        Long brandId,
        String brandName
    ) {
        public static ProductInfo from(ProductView productView) {
            return new ProductInfo(
                productView.getId(),
                productView.getName(),
                productView.getPrice().longValue(),
                productView.getLikeCount(),
                productView.getBrandId(),
                productView.getBrandName()
            );
        }
    }

    /**
     * 페이지 응답
     */
    public record PageResponse<T>(
        List<T> content,
        PageInfo page
    ) {
        public static <T> PageResponse<T> from(Page<T> page) {
            return new PageResponse<>(
                page.getContent(),
                new PageInfo(
                    page.getNumber(),
                    page.getSize(),
                    page.getTotalElements(),
                    page.getTotalPages()
                )
            );
        }
    }

    /**
     * 페이지 정보
     */
    public record PageInfo(
        Integer page,
        Integer size,
        Long totalElements,
        Integer totalPages
    ) {}
}



