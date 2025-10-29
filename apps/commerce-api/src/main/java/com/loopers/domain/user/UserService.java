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
    public UserModel accountUser(String userId, String email, String birthdate, String gender) {
        if( userRepository.existsByUserId(userId) ) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이미 존재하는 ID 입니다.");
        }

        UserModel user = UserModel.builder()
                .userId(userId)
                .email(email)
                .birthdate(birthdate)
                .gender(gender)
                .build();

        return userRepository.save(user);
    }
}
