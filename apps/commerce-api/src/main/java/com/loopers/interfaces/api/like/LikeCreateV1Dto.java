package com.loopers.interfaces.api.like;

import com.loopers.application.user.UserInfo;

public class LikeCreateV1Dto {
  public record UserRequest(String userId, String email, String birthday, String gender) {
  }

  public record UserResponse(String userId, String email, String birthday, String gender) {
    public static UserResponse from(UserInfo info) {
      if (info == null) return null;
      return new UserResponse(
          info.userId(),
          info.email(),
          info.birthday(),
          info.gender()
      );
    }
  }
}
