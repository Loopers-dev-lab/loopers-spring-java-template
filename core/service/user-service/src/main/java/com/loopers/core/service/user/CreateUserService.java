package com.loopers.core.service.user;

import com.loopers.core.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateUserService {

    public User createUser(User user) {
        return user;
    }
}
