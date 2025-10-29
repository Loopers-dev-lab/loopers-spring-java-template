package com.loopers.domain.user;

import com.loopers.application.user.UserCommand;
import com.loopers.application.user.UserInfo;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserModel getExample(String userId) {
        return userRepository.findByUserId(userId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "[userId = " + userId + "] 예시를 찾을 수 없습니다."));
    }

    @Transactional
    public UserModel createUser(UserCommand.Create newUser) {

        if(userRepository.existsUserId(newUser.userId())) {
            throw new CoreException(ErrorType.CONFLICT, "이미 사용중인 이용자ID 입니다.");
        }

        return userRepository.save(newUser.toModel());
    }
}
