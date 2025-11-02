package com.loopers.interfaces.api.userinfo;

import com.loopers.domain.user.User;

/**
 * 사용자 정보 API v1의 데이터 전송 객체(DTO) 컨테이너.
 *
 * @author Loopers
 * @version 1.0
 */
public class UserInfoV1Dto {
    /**
     * 사용자 정보 응답 데이터.
     *
     * @param userId 사용자 ID
     * @param email 이메일 주소
     * @param birthDate 생년월일 (문자열)
     * @param gender 성별
     */
    public record UserInfoResponse(
        String userId,
        String email,
        String birthDate,
        String gender
    ) {
        /**
         * User 엔티티로부터 UserInfoResponse를 생성합니다.
         *
         * @param user 사용자 엔티티
         * @return 생성된 응답 객체
         */
        public static UserInfoResponse from(User user) {
            return new UserInfoResponse(
                user.getUserId(),
                user.getEmail(),
                user.getBirthDate().toString(),
                user.getGender() != null ? user.getGender().name() : null
            );
        }
    }
}
