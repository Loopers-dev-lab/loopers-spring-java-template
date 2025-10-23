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
    public UserModel createUser(String id, String email, String birthDate, String gender) {
        if (userRepository.existsById(id)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이미 가입된 ID입니다.");
        }
        
        UserModel user = new UserModel(id, email, birthDate, gender);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public UserModel getUserById(String id) {
        return userRepository.findById(id)
            .orElse(null);
    }

    @Transactional(readOnly = true)
    public Integer getUserPoint(String id) {
        UserModel user = userRepository.findById(id)
            .orElse(null);
        return user != null ? user.getPoint() : null;
    }

    @Transactional
    public UserModel chargePoint(String id, Integer amount) {
        UserModel user = userRepository.findById(id)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        
        user.chargePoint(amount);
        return userRepository.save(user);
    }
}
