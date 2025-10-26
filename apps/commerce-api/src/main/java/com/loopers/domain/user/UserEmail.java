package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import java.util.regex.Pattern;
import lombok.Getter;

@Getter
public class UserEmail {

  private static final Pattern VALID_EMAIL_PATTERN = Pattern.compile("^[\\w.+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");

  private final String email;

  public UserEmail(String email) {
    if (email == null || email.isBlank()) {
      throw new CoreException(ErrorType.BAD_REQUEST, "이메일은 비어있을 수 없습니다.");
    }

    if (!VALID_EMAIL_PATTERN.matcher(email).matches()) {
      throw new CoreException(ErrorType.BAD_REQUEST, "이메일 형식이 올바르지 않습니다.");
    }
    this.email = email;
  }
}
