package com.loopers.application.ranking;

import com.loopers.domain.ranking.RankingType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
public class JobParameterUtils {

    private static final String TARGET_DATE_PARAM = "targetDate";
    private static final String RANKING_TYPE_PARAM = "rankingType";

    public static LocalDate getTargetDate(StepExecution stepExecution) {
        if (stepExecution == null) {
            return LocalDate.now().minusDays(1);
        }

        String targetDateStr = stepExecution.getJobParameters().getString(TARGET_DATE_PARAM);
        if (targetDateStr == null || targetDateStr.isEmpty()) {
            return LocalDate.now().minusDays(1);
        }

        try {
            return LocalDate.parse(targetDateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
        } catch (Exception e) {
            log.warn("targetDate 형식이 올바르지 않습니다: {}", targetDateStr);
            return LocalDate.now().minusDays(1);
        }
    }

    public static RankingType getRankingType(StepExecution stepExecution) {
        if (stepExecution == null) {
            return null;
        }

        String rankingTypeStr = stepExecution.getJobParameters().getString(RANKING_TYPE_PARAM);
        if (rankingTypeStr == null || rankingTypeStr.isEmpty()) {
            return RankingType.WEEKLY;
        }

        return RankingType.valueOf(rankingTypeStr.toUpperCase());
    }
}

