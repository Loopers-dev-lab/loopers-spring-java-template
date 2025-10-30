package com.loopers.domain.point;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.user.Gender;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.regex.Pattern;

@Embeddable
public class Point {
    private long amount;

    protected Point() { }

    private Point(long amount) {
        validateAmount(amount);

        this.amount = amount;
    }

    private static void validateAmount(long amount) {
        if (amount < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "포인트는 0원 이상이여야 합니다.");
        }
    }

    public static Point zero() { return new Point(0L); }

    public static Point of(long amount) { return new Point(amount); }

    public long amount() { return amount; }

}
