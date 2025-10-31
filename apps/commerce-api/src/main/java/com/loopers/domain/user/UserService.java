package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * 사용자 도메인 서비스.
 * <p>
 * 사용자 생성 및 조회 등의 도메인 로직을 처리합니다.
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
@RequiredArgsConstructor
@Component
public class UserService {
    private final UserRepository userRepository;

    /**
     * 새로운 사용자를 생성합니다.
     *
     * @param userId 사용자 ID
     * @param email 이메일 주소
     * @param birthDateStr 생년월일 (yyyy-MM-dd)
     * @param gender 성별
     * @return 생성된 사용자
     * @throws CoreException 중복된 사용자 ID가 존재하거나 유효성 검증 실패 시
     */
    public User create(String userId, String email, String birthDateStr, Gender gender) {
        User user = User.of(userId, email, birthDateStr, gender);
        try {
            return userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            if (e.getMessage() != null && e.getMessage().contains("user_id")) {
                throw new CoreException(ErrorType.CONFLICT, "이미 가입된 ID입니다: " + userId);
            }
            throw new CoreException(ErrorType.CONFLICT, "데이터 무결성 제약 조건 위반");
        }
    }

    /**
     * 사용자 ID로 사용자를 조회합니다.
     *
     * @param userId 조회할 사용자 ID
     * @return 조회된 사용자, 없으면 null
     */
    public User findByUserId(String userId) {
        return userRepository.findByUserId(userId);
    }
}
