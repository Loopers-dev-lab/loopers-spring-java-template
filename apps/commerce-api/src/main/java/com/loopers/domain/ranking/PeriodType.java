package com.loopers.domain.ranking;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PeriodType {
    DAILY("일간", "오늘 또는 특정 날짜"),
    WEEKLY("주간", "이번 주 또는 특정 주차"),
    MONTHLY("월간", "이번 달 또는 특정 월");

    private final String name;
    private final String description;
}
