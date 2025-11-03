package com.loopers.core.domain.user.vo;

import com.loopers.core.domain.error.DomainErrorCode;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

public record UserBirthDay(LocalDate value) {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public UserBirthDay(LocalDate value) {
        Objects.requireNonNull(value, DomainErrorCode.notNullMessage("생년월일"));
        this.value = value;
    }

    public static UserBirthDay create(String dateString) {
        Objects.requireNonNull(dateString, DomainErrorCode.notNullMessage("생년월일"));
        validate(dateString);

        LocalDate parsedDate = LocalDate.parse(dateString, DATE_FORMATTER);
        return new UserBirthDay(parsedDate);
    }

    private static void validate(String dateString) {
        try {
            DateTimeFormatter.ofPattern("yyyy-MM-dd").parse(dateString);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(DomainErrorCode.INVALID_BIRTHDATE_FORMAT.getMessage());
        }
    }
}
