package com.loopers.application.userinfo;

import com.loopers.domain.user.User;
import com.loopers.domain.user.UserRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 사용자 정보 조회 파사드.
 * <p>
 * 사용자 정보 조회 유즈케이스를 처리하는 애플리케이션 서비스입니다.
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
@RequiredArgsConstructor
@Component
public class UserInfoFacade {
    private final UserRepository userRepository;

    /**
     * 사용자 ID로 사용자 정보를 조회합니다.
     *
     * @param userId 조회할 사용자 ID
     * @return 조회된 사용자 정보
     * @throws CoreException 사용자를 찾을 수 없는 경우
     */
    public UserInfo getUserInfo(String userId) {
        User user = userRepository.findByUserId(userId);
        if (user == null) {
            throw new CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다.");
        }
        return UserInfo.from(user);
    }

    /**
     * 사용자 정보를 담는 레코드.
     *
     * @param userId 사용자 ID
     * @param email 이메일 주소
     * @param birthDate 생년월일
     * @param gender 성별
     */
    public record UserInfo(
        String userId,
        String email,
        java.time.LocalDate birthDate,
        com.loopers.domain.user.Gender gender
    ) {
        /**
         * User 엔티티로부터 UserInfo를 생성합니다.
         *
         * @param user 사용자 엔티티
         * @return 생성된 UserInfo
         */
        public static UserInfo from(User user) {
            return new UserInfo(
                user.getUserId(),
                user.getEmail(),
                user.getBirthDate(),
                user.getGender()
            );
        }
    }
}

