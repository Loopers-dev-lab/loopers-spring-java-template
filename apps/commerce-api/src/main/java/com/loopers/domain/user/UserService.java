package com.loopers.domain.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class UserService {

    private final UserRepository userRepository;

    public void register(String id, String email, String birth) {
        userRepository.save(UserModel.create(id, email, birth));
    }
}
