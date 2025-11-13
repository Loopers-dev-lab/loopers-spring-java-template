package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public UserEntity findUserByLoginId(String loginId) {
        return userRepository.findUserByLoginId(loginId).orElseThrow(
                () -> new CoreException(ErrorType.NOT_FOUND, "해당 이메일의 사용자가 존재하지 않습니다.")
        );
    }

    public UserEntity save(UserEntity entity) {
        if (existsByLoginId(entity.getLoginId())) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이미 존재하는 유저ID입니다.");
        }

        return userRepository.save(entity);
    }

    public boolean existsByLoginId(String loginId) {
        return userRepository.existsByLoginId(loginId);
    }

    public void checkUserExists(Long id) {
        userRepository.findById(id).orElseThrow(
                () -> new CoreException(ErrorType.NOT_FOUND, "해당 ID의 사용자가 존재하지 않습니다.")
        );
    }

}
