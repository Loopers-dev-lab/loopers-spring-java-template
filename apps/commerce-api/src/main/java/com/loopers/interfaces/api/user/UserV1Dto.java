package com.loopers.interfaces.api.user;

import com.loopers.application.user.UserInfo;
import java.time.LocalDate;

public class UserV1Dto {
    
    public record UserCreateRequest(String id, String email, String birthDate, String gender) {}
    
    public record UserResponse(String id, String email, LocalDate birthDate, String gender, Integer point) {
        public static UserResponse from(UserInfo info) {
            return new UserResponse(
                info.id(),
                info.email(),
                info.birthDate(),
                info.gender(),
                info.point()
            );
        }
    }
    
    public record PointResponse(Integer point) {
        public static PointResponse from(Integer point) {
            return new PointResponse(point);
        }
    }
    
    public record PointChargeRequest(Integer amount) {}
}
