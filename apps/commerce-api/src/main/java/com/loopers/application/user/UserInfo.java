package com.loopers.application.user;

import com.loopers.domain.user.User;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

public record UserInfo(String userId, String email, String birthday, String gender) {
  public static UserInfo from(User model) {
    if (model == null) throw new CoreException(ErrorType.NOT_FOUND, "유저정보를 찾을수 없습니다.");
    return new UserInfo(
        model.getLoginId(),
        model.getEmail(),
        model.getBirthday().toString(),
        model.getGender()
    );
  }
}
