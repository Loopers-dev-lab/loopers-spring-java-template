package com.loopers.application.user;

import com.loopers.domain.user.User;

public record UserInfo(String userId, String email, String birthday, String gender) {
  public static UserInfo from(User model) {
    return new UserInfo(
        model.getUserId(),
        model.getEmail(),
        model.getBirthday().toString(),
        model.getGender()
    );
  }
}
