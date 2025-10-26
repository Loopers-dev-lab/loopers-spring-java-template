package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import java.util.regex.Pattern;
import lombok.Getter;

@Getter
public class UserId {

  private static final Pattern VALID_USER_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9]{1,10}$");

  private final String id;

  public UserId(String id) {
    if (id == null || id.isBlank()) {
      throw new CoreException(ErrorType.BAD_REQUEST, "사용자 ID는 비어있을 수 없습니다.");
    }

    if (!VALID_USER_ID_PATTERN.matcher(id).matches()) {
      throw new CoreException(ErrorType.BAD_REQUEST, "사용자 ID는 영문 및 숫자 10자 이내여야 합니다.");
    }
    this.id = id;
  }
}
