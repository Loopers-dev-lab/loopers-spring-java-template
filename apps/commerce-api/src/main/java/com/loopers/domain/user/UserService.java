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

    @Transactional( readOnly = true )
    public UserModel getUserByUserId(String userId) {

        return userRepository.findUserByUserId(userId)
                .orElse(null);

    }

    @Transactional( readOnly = true )
    public Integer getUserPointByUserId(String userId) {
        UserModel user = userRepository.findUserByUserId(userId)
                .orElse(null);

        return user != null ? user.getPoint() : null;
    }
}
