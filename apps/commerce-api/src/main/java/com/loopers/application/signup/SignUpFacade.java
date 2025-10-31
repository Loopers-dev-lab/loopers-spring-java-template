package com.loopers.application.signup;

import com.loopers.domain.point.PointService;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 회원가입 파사드.
 * <p>
 * 회원가입 시 사용자 생성과 포인트 초기화를 조율하는 
 * 애플리케이션 서비스입니다.
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

    private final PointService pointService;

    /**
     * 회원가입을 처리합니다.
     * <p>
     * 사용자를 생성하고 초기 포인트(0)를 부여합니다.
     * 전체 과정이 하나의 트랜잭션으로 처리됩니다.
     * </p>
     *
     * @param userId 사용자 ID
     * @param email 이메일 주소
     * @param birthDateStr 생년월일 (yyyy-MM-dd)
     * @param gender 성별
     * @return 생성된 사용자 정보
     * @throws com.loopers.support.error.CoreException 유효성 검증 실패 또는 중복 ID 존재 시
     */
    @Transactional
    public SignUpInfo signUp(String userId, String email, String birthDateStr, Gender gender) {
        User user = userService.create(userId, email, birthDateStr, gender);
        pointService.create(user, 0L);
        return SignUpInfo.from(user);
    }
}
