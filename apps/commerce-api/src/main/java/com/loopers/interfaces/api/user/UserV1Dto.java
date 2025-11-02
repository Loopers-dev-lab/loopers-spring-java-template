package com.loopers.interfaces.api.user;

import com.loopers.application.user.UserCommand;
import com.loopers.application.user.UserInfo;

public class UserV1Dto {

    public record UserCreateRequest(
            String userId,
            String userName,
            String description,
            String email,
            String birthDate,
            String gender,
            Integer point
    ) {
        /**
         * interfaces -> application 의존 OK(안쪽 의존)
         *
         * @return
         */
        public UserCommand.Create toCommand() {
            return new UserCommand.Create(userId, userName, description, email, birthDate, gender, point);
        }
    }

    public record UserPointChargeRequest(
            String userId,
            Integer point
    ) {
    }

    public record UserResponse(
            String userId,
            String userName,
            String description,
            String email,
            String birthDate,
            String gender,
            Integer point
    ) {
        public static UserResponse from(UserInfo info) {
            return new UserResponse(
                    info.userId(),
                    info.userName(),
                    info.description(),
                    info.email(),
                    info.birthDate(),
                    info.gender(),
                    info.point()
            );
        }
    }
}
