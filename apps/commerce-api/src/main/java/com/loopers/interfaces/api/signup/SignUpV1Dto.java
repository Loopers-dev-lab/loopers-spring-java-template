package com.loopers.interfaces.api.signup;

import com.loopers.application.signup.SignUpInfo;
import jakarta.validation.constraints.NotBlank;

/**
 * 회원가입 API v1의 데이터 전송 객체(DTO) 컨테이너.
 *
 * @author Loopers
 * @version 1.0
 */
public class SignUpV1Dto {
    /**
     * 회원가입 요청 데이터.
     *
     * @param userId 사용자 ID (필수)
     * @param email 이메일 주소 (필수)
     * @param birthDate 생년월일 (필수, yyyy-MM-dd)
     * @param gender 성별 (필수, MALE 또는 FEMALE)
     */
    public record SignUpRequest(
        @NotBlank String userId,
        @NotBlank String email,
        @NotBlank String birthDate,
        @NotBlank String gender
    ) {}

    /**
     * 회원가입 응답 데이터.
     *
     * @param id 사용자 엔티티 ID
     * @param userId 사용자 ID
     * @param email 이메일 주소
     * @param birthDate 생년월일
     * @param gender 성별
     */
    public record SignupResponse(Long id, String userId, String email, String birthDate, String gender) {
        /**
         * SignUpInfo로부터 SignupResponse를 생성합니다.
         *
         * @param info 회원가입 정보
         * @return 생성된 응답 객체
         */
        public static SignupResponse from(SignUpInfo info) {
            return new SignupResponse(
                info.id(),
                info.userId(),
                info.email(),
                info.birthDate().toString(),
                info.gender() != null ? info.gender().name() : null
            );
        }
    }
}