package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public User register(String userId, String email, String birth, String gender) {
        userRepository.findByUserId(userId).ifPresent(user -> {
            throw new CoreException(ErrorType.CONFLICT, "이미 가입된 사용자ID 입니다.");
        });

        User user = new User(userId, email, birth, gender);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User findUserByUserId(String userId) {
        return userRepository.findByUserId(userId).orElse(null);
    }

}
