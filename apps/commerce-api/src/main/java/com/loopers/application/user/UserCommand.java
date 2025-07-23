package com.loopers.application.user;

import com.loopers.domain.user.UserEntity;
import com.loopers.interfaces.api.user.UserV1Dto;


public class UserCommand {
    public record CreateUserRequest(
            String loginId,
            String email,
            String birth,
            String gender){
        public static CreateUserRequest from(UserV1Dto.CreateUserRequest request) {
            return new CreateUserRequest(
                    request.loginId(),
                    request.email(),
                    request.birth(),
                    request.gender()
            );
        }
    }
    public record UserResponse(
            Long userId,
            String loginId,
            String email,
            String birth,
            String gender){
        public static UserResponse from(UserEntity user) {
            return new UserResponse(
                    user.getId(),
                    user.getLoginId(),
                    user.getEmail(),
                    user.getBirth(),
                    user.getGrender()
            );
        }
    }


}
