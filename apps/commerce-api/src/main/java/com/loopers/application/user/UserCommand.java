package com.loopers.application.user;

import com.loopers.domain.user.UserModel;

/**
 * application에서 domain 레이어로 향하는 쓰기 관련 DTO들을 정의
 */
public class UserCommand {
    public record Create (
            String userId,
            String userName,
            String description,
            String email,
            String birthDate,
            String gender,
            Integer point
    ) {
        public UserModel toModel() {
            return new UserModel(
                    userId,
                    userName,
                    description,
                    email,
                    birthDate,
                    gender,
                    point);
        }
    }
}
