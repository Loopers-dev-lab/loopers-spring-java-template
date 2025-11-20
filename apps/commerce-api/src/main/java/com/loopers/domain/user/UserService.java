package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Component
public class UserService {
    private final UserRepository userRepository;

    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }

    public boolean existsByUserId(final String userId) {
        return userRepository.existsByUserId(userId);
    }

    public User getUser(final Long id) {
        return userRepository.find(id)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND,  "[id = " + id + "] 유저를 찾을 수 없습니다."));
    }
}
