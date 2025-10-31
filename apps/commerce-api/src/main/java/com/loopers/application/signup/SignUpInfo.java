package com.loopers.application.signup;

import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;

import java.time.LocalDate;

/**
 * 회원가입 결과 정보를 담는 레코드.
 * <p>
 * User 도메인 엔티티로부터 생성된 불변 데이터 전송 객체입니다.
 * </p>
 *
 * @param id 사용자 엔티티 ID
 * @param userId 사용자 ID
 * @param email 이메일 주소
 * @param birthDate 생년월일
 * @param gender 성별
 * @author Loopers
 * @version 1.0
 */
public record SignUpInfo(Long id, String userId, String email, LocalDate birthDate, Gender gender) {
    /**
     * User 엔티티로부터 SignUpInfo를 생성합니다.
     *
     * @param user 변환할 사용자 엔티티
     * @return 생성된 SignUpInfo
     */
    public static SignUpInfo from(User user) {
        return new SignUpInfo(
                user.getId(),
                user.getUserId(),
                user.getEmail(),
                user.getBirthDate(),
                user.getGender()
        );
    }
}
