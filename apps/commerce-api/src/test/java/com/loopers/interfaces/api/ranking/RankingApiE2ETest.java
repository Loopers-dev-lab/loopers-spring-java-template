package com.loopers.interfaces.api.ranking;

import com.loopers.domain.ranking.MonthlyRankEntity;
import com.loopers.domain.ranking.MonthlyRankRepository;
import com.loopers.domain.ranking.WeeklyRankEntity;
import com.loopers.domain.ranking.WeeklyRankRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 랭킹 API E2E 테스트
 * - 실제 HTTP 요청부터 DB 조회까지 전체 플로우 검증
 */
@SpringBootTest
@AutoConfigureWebMvc
@DisplayName("랭킹 API E2E 테스트")
class RankingApiE2ETest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private WeeklyRankRepository weeklyRankRepository;

    @Autowired
    private MonthlyRankRepository monthlyRankRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // 테스트 데이터 준비
        setupTestData();
    }

    @AfterEach
    void tearDown() {
        // 테스트 데이터 정리
        weeklyRankRepository.deleteByYearWeek("2024-W52");
        monthlyRankRepository.deleteByYearMonth("2024-12");
    }

    private void setupTestData() {
        // 주간 랭킹 테스트 데이터
        List<WeeklyRankEntity> weeklyRankings = List.of(
                WeeklyRankEntity.create(1L, "2024-W52", 1000L, 500L, 100L, 50L, 3100L, 1),
                WeeklyRankEntity.create(2L, "2024-W52", 800L, 400L, 80L, 40L, 2480L, 2),
                WeeklyRankEntity.create(3L, "2024-W52", 600L, 300L, 60L, 30L, 1860L, 3)
        );
        weeklyRankRepository.saveAll(weeklyRankings);

        // 월간 랭킹 테스트 데이터
        List<MonthlyRankEntity> monthlyRankings = List.of(
                MonthlyRankEntity.create(1L, "2024-12", 5000L, 2500L, 500L, 250L, 15500L, 1),
                MonthlyRankEntity.create(2L, "2024-12", 4000L, 2000L, 400L, 200L, 12400L, 2),
                MonthlyRankEntity.create(3L, "2024-12", 3000L, 1500L, 300L, 150L, 9300L, 3)
        );
        monthlyRankRepository.saveAll(monthlyRankings);
    }

    @Nested
    @DisplayName("주간 랭킹 API")
    class 주간_랭킹_API {

        @Test
        @DisplayName("주간 랭킹 조회 API가 정상적으로 동작한다")
        void should_return_weekly_rankings_successfully() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/rankings/period")
                            .param("period", "WEEKLY")
                            .param("yearWeek", "2024-W52")
                            .param("size", "10")
                            .param("page", "0"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content.length()").value(3))
                    .andExpect(jsonPath("$.data.totalElements").value(3))
                    .andExpect(jsonPath("$.data.content[0].id").value(1))  // 1위 상품
                    .andExpect(jsonPath("$.data.content[1].id").value(2))  // 2위 상품
                    .andExpect(jsonPath("$.data.content[2].id").value(3)); // 3위 상품
        }

        @Test
        @DisplayName("존재하지 않는 주차 조회 시 빈 결과를 반환한다")
        void should_return_empty_result_for_non_existent_year_week() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/rankings/period")
                            .param("period", "WEEKLY")
                            .param("yearWeek", "2024-W01")
                            .param("size", "10")
                            .param("page", "0"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content.length()").value(0))
                    .andExpect(jsonPath("$.data.totalElements").value(0));
        }

        @Test
        @DisplayName("yearWeek 파라미터 누락 시 400 에러를 반환한다")
        void should_return_400_when_year_week_parameter_is_missing() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/rankings/period")
                            .param("period", "WEEKLY")
                            .param("size", "10")
                            .param("page", "0"))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("페이지네이션이 정상적으로 동작한다")
        void should_support_pagination_correctly() throws Exception {
            // when & then - 첫 번째 페이지 (size=2)
            mockMvc.perform(get("/api/v1/rankings/period")
                            .param("period", "WEEKLY")
                            .param("yearWeek", "2024-W52")
                            .param("size", "2")
                            .param("page", "0"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(2))
                    .andExpect(jsonPath("$.data.totalElements").value(3))
                    .andExpect(jsonPath("$.data.totalPages").value(2))
                    .andExpect(jsonPath("$.data.first").value(true))
                    .andExpect(jsonPath("$.data.last").value(false));

            // when & then - 두 번째 페이지 (size=2)
            mockMvc.perform(get("/api/v1/rankings/period")
                            .param("period", "WEEKLY")
                            .param("yearWeek", "2024-W52")
                            .param("size", "2")
                            .param("page", "1"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.totalElements").value(3))
                    .andExpect(jsonPath("$.data.totalPages").value(2))
                    .andExpect(jsonPath("$.data.first").value(false))
                    .andExpect(jsonPath("$.data.last").value(true));
        }
    }

    @Nested
    @DisplayName("월간 랭킹 API")
    class 월간_랭킹_API {

        @Test
        @DisplayName("월간 랭킹 조회 API가 정상적으로 동작한다")
        void should_return_monthly_rankings_successfully() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/rankings/period")
                            .param("period", "MONTHLY")
                            .param("yearMonth", "2024-12")
                            .param("size", "10")
                            .param("page", "0"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content.length()").value(3))
                    .andExpect(jsonPath("$.data.totalElements").value(3))
                    .andExpect(jsonPath("$.data.content[0].id").value(1))  // 1위 상품
                    .andExpect(jsonPath("$.data.content[1].id").value(2))  // 2위 상품
                    .andExpect(jsonPath("$.data.content[2].id").value(3)); // 3위 상품
        }

        @Test
        @DisplayName("존재하지 않는 월 조회 시 빈 결과를 반환한다")
        void should_return_empty_result_for_non_existent_year_month() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/rankings/period")
                            .param("period", "MONTHLY")
                            .param("yearMonth", "2024-01")
                            .param("size", "10")
                            .param("page", "0"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content.length()").value(0))
                    .andExpect(jsonPath("$.data.totalElements").value(0));
        }

        @Test
        @DisplayName("yearMonth 파라미터 누락 시 400 에러를 반환한다")
        void should_return_400_when_year_month_parameter_is_missing() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/rankings/period")
                            .param("period", "MONTHLY")
                            .param("size", "10")
                            .param("page", "0"))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("일간 랭킹 API (기존)")
    class 일간_랭킹_API {

        @Test
        @DisplayName("일간 랭킹 API가 정상적으로 동작한다 (하위 호환성)")
        void should_return_daily_rankings_successfully() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/rankings")
                            .param("size", "10")
                            .param("page", "0"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray());
        }

        @Test
        @DisplayName("새로운 period 방식으로도 일간 랭킹 조회가 가능하다")
        void should_return_daily_rankings_with_period_parameter() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/rankings/period")
                            .param("period", "DAILY")
                            .param("size", "10")
                            .param("page", "0"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray());
        }
    }

    @Nested
    @DisplayName("파라미터 검증")
    class 파라미터_검증 {

        @Test
        @DisplayName("잘못된 period 값 시 400 에러를 반환한다")
        void should_return_400_for_invalid_period() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/rankings/period")
                            .param("period", "INVALID")
                            .param("size", "10")
                            .param("page", "0"))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("음수 페이지 번호 시 400 에러를 반환한다")
        void should_return_400_for_negative_page_number() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/rankings/period")
                            .param("period", "WEEKLY")
                            .param("yearWeek", "2024-W52")
                            .param("size", "10")
                            .param("page", "-1"))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("0 이하의 size 값 시 400 에러를 반환한다")
        void should_return_400_for_invalid_size() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/rankings/period")
                            .param("period", "WEEKLY")
                            .param("yearWeek", "2024-W52")
                            .param("size", "0")
                            .param("page", "0"))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }
}