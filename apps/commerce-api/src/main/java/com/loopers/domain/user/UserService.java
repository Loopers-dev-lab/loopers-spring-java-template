package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public User registerUser(String id, String email, String birthDate, String gender) {
        if (userRepository.existsById(id)) {
            throw new CoreException(ErrorType.CONFLICT, "이미 존재하는 ID입니다: " + id);
        }

        User user = User.create(id, email, birthDate, gender);
        return userRepository.save(user);
    }
}
