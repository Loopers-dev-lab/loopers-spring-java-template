package com.loopers.application.ranking;

import java.time.LocalDate;

public class RankingCommand {

    public enum RankingType {
        DAILY,
        WEEKLY,
        MONTHLY
    }

    public record GetRankingCommand(
            LocalDate date,
            RankingType type,
            int page,
            int size
    ) {
    }

}
