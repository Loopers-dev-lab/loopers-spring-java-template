package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class UserService {

    private final UserRepository userRepository;

    public void register(String id, String email, String birth) {
        if (userRepository.existsByUserId(id)) {
            throw new CoreException(ErrorType.CONFLICT, "중복된 ID 입니다.");
        }

        userRepository.save(UserModel.create(id, email, birth));
    }
}
