package com.loopers.domain.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum Gender {
    MALE("남성"),
    FEMALE("여성");

    private final String text;
}
