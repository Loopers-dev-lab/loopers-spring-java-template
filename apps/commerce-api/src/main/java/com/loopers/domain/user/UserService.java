package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class UserService {

    private final UserRepository userRepository;

     @Transactional(readOnly = true)
     public UserModel getUser(String userId) {
         return userRepository.find(userId).orElse(null);
     }

    @Transactional
    public UserModel signUp(UserModel userModel) {
        Optional<UserModel> user = userRepository.find(userModel.getUserId());

        if (user.isPresent()) {
            throw new CoreException(ErrorType.CONFLICT, "[userId = " + userModel.getUserId() + "] 아이디가 중복되었습니다.");
        }
        return userRepository.save(userModel);
    }
}
