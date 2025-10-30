package com.loopers.domain.user;

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

    public UserInfo register(String id, String email, String birth, Gender gender) {
        if (userRepository.existsByUserId(id)) {
            throw new CoreException(ErrorType.CONFLICT, "중복된 ID 입니다.");
        }

        UserModel user = userRepository.save(UserModel.create(id, email, birth, gender));

        return UserInfo.from(user);
    }

    @Transactional(readOnly = true)
    public UserModel getUser(String userId) {
        return userRepository.find(userId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "[id = " + userId + "] 를 찾을 수 없습니다."));
    }

}
