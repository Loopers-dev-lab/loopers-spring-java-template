package com.loopers.application.user;

import com.loopers.domain.user.UserModel;

public record UserInfo(String userId, String email, String birthday, String gender) {
  public static UserInfo from(UserModel model) {
    if (model == null) return null;
    return new UserInfo(
        model.getUserId(),
        model.getEmail(),
        model.getBirthday().toString(),
        model.getGender()
    );
  }
}
