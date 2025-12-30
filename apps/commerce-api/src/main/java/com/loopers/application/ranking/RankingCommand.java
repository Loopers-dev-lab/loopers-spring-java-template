package com.loopers.application.ranking;

import java.time.LocalDate;

public class RankingCommand {

    public record GetDailyRankingCommand(
            LocalDate date,
            int page,
            int size
    ) {
    }

}
