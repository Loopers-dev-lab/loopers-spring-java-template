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
    public UserModel getUserOrNull(String userId) {
        return userRepository.findByUserId(userId).orElse(null);

    }

    @Transactional
    public UserModel createUser(UserCommand.Create newUser) {

        if(userRepository.existsUserId(newUser.userId())) {
            throw new CoreException(ErrorType.CONFLICT, "이미 사용중인 이용자ID 입니다.");
        }

        return userRepository.save(newUser.toModel());
    }

    @Transactional(readOnly = true)
    public Integer getUserPoint(String userId) {
        return userRepository.findByUserId(userId)
                .map(UserModel::getPoint)
                .orElse(null);
    }

    public Integer chargePoint(String userId, Integer point) {
        UserModel user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "이용자ID를 확인해주세요."));
        user.updatePoint(point);
        userRepository.save(user);

        return user.getPoint();
    }
}
