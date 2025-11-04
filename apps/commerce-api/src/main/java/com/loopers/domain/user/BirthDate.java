package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BirthDate {

    @Column(nullable = false)
    private LocalDate date;

    public BirthDate(String date) {
        validate(date);
        this.date = LocalDate.parse(date);
    }

    private BirthDate(LocalDate date) {
        this.date = date;
    }

    private void validate(String birthDate) {
        if (birthDate == null || birthDate.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "생년월일은 필수입니다.");
        }
        try {
            LocalDate.parse(birthDate);
        } catch (DateTimeParseException e) {
            throw new CoreException(ErrorType.BAD_REQUEST, "올바른 날짜 형식이 아닙니다. (yyyy-MM-dd)");
        }
    }

    public static BirthDate from(LocalDate localDate) {
        return new BirthDate(localDate);
    }
}
