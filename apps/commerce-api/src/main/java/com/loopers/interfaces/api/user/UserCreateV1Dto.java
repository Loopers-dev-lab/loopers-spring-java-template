package com.loopers.interfaces.api.user;

import com.loopers.application.user.UserInfo;

import java.time.LocalDate;

public class UserCreateV1Dto {
  public record UserRequest(String userId, String email, String birthday, String gender) {
  }

  public record UserResponse(String userId, String email, String birthday, String gender) {
    public static UserResponse from(UserInfo info) {
      return new UserResponse(
          info.userId(),
          info.email(),
          info.birthday().toString(),
          info.gender()
      );
    }
  }
}
