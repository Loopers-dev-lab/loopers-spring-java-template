package com.loopers.application.ranking.batch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.ranking.MvProductRankMonthly;
import com.loopers.domain.ranking.MvProductRankWeekly;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RankingJsonConverter {

    private final ObjectMapper objectMapper;

    public String toJson(List items) throws JsonProcessingException {
        return objectMapper.writeValueAsString(items);
    }

    public List<MvProductRankWeekly> fromJsonWeekly(String json) throws JsonProcessingException {
        return objectMapper.readValue(
                json,
                objectMapper.getTypeFactory().constructCollectionType(
                        List.class,
                        MvProductRankWeekly.class
                )
        );
    }

    public List<MvProductRankMonthly> fromJsonMonthly(String json) throws JsonProcessingException {
        return objectMapper.readValue(
                json,
                objectMapper.getTypeFactory().constructCollectionType(
                        List.class,
                        MvProductRankMonthly.class
                )
        );
    }
}
