package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    @Transactional
    public User saveUser(User user) {
        if (userRepository.findByUserId(user.getUserId()).isPresent()){
            throw new CoreException(ErrorType.BAD_REQUEST,"이미 존재하는 ID 입니다.");
        }
        return userRepository.save(user);
    }

    @Transactional
    public User getUser(String id) {
        return userRepository.findByUserId(id)
        .orElse(null);
    }
}
