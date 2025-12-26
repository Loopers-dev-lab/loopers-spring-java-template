package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class UserService {
    private final UserRepository userRepository;

    @Transactional
    public User registerUser(String userId, String email, String birthday, String gender) {
        // 이미 등록된 userId 인 경우, 예외를 발생시킨다.
        if (userRepository.existsUserByUserId(userId)) {
            throw new CoreException(ErrorType.CONFLICT, "이미 존재하는 사용자 ID 입니다.");
        }
        User user = User.create(userId, email, birthday, gender);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByUserId(String userId) {
        return userRepository.findByUserId(userId);
    }

    // find by user id with lock for update
    @Transactional
    public Optional<User> findByUserIdForUpdate(String userId) {
        return userRepository.findByUserIdForUpdate(userId);
    }

}
