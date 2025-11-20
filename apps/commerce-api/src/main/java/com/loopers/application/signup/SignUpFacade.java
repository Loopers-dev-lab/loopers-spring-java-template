package com.loopers.application.signup;

import com.loopers.domain.user.Gender;
import com.loopers.domain.user.Point;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 회원가입 파사드.
 * <p>
 * 회원가입 시 사용자 생성을 처리하는 애플리케이션 서비스입니다.
 * 사용자 생성 시 포인트는 자동으로 0으로 초기화됩니다.
 * 트랜잭션 경계를 관리하여 데이터 일관성을 보장합니다.
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
@RequiredArgsConstructor
@Component
public class SignUpFacade {
    private final UserService userService;

    /**
     * 회원가입을 처리합니다.
     * <p>
     * 사용자를 생성하며, 포인트는 자동으로 0으로 초기화됩니다.
     * 전체 과정이 하나의 트랜잭션으로 처리됩니다.
     * </p>
     *
     * @param userId 사용자 ID
     * @param email 이메일 주소
     * @param birthDateStr 생년월일 (yyyy-MM-dd)
     * @param genderStr 성별 문자열 (MALE 또는 FEMALE)
     * @return 생성된 사용자 정보
     * @throws CoreException gender 값이 유효하지 않거나, 유효성 검증 실패 또는 중복 ID 존재 시
     */
    @Transactional
    public SignUpInfo signUp(String userId, String email, String birthDateStr, String genderStr) {
        Gender gender = parseGender(genderStr);
        Point point = Point.of(0L);
        User user = userService.create(userId, email, birthDateStr, gender, point);
        return SignUpInfo.from(user);
    }

    /**
     * 성별 문자열을 Gender enum으로 변환합니다.
     * <p>
     * 도메인 진입점에서 방어 로직을 제공하여 NPE를 방지합니다.
     * </p>
     *
     * @param genderStr 성별 문자열
     * @return Gender enum
     * @throws CoreException gender 값이 null이거나 유효하지 않은 경우
     */
    private Gender parseGender(String genderStr) {
        if (genderStr == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "gender 값이 올바르지 않습니다.");
        }
        try {
            String genderValue = genderStr.trim().toUpperCase(Locale.ROOT);
            return Gender.valueOf(genderValue);
        } catch (IllegalArgumentException e) {
            throw new CoreException(ErrorType.BAD_REQUEST, "gender 값이 올바르지 않습니다.");
        }
    }
}
