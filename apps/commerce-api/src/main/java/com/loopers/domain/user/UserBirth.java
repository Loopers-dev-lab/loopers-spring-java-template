package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import lombok.Getter;

@Getter
public class UserBirth {

  private static final DateTimeFormatter BIRTH_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  private final LocalDate birth;

  public UserBirth(String dateString) {
    if (dateString == null || dateString.isBlank()) {
      throw new CoreException(ErrorType.BAD_REQUEST, "생년월일은 비어있을 수 없습니다.");
    }
    this.birth = parse(dateString);
  }

  private LocalDate parse(String dateString) {
    try {
      return LocalDate.parse(dateString, BIRTH_DATE_FORMATTER);
    } catch (DateTimeParseException e) {
      throw new CoreException(ErrorType.BAD_REQUEST, "생년월일 형식이 올바르지 않습니다. (yyyy-MM-dd)");
    }
  }
}
