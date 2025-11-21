
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

  @Transactional
  public UserModel join(UserModel user) {
    if (userRepository.existsByUserId(user.getUserId())) {
      throw new CoreException(ErrorType.BAD_REQUEST, "이미 가입된 ID 입니다.");
    }
    return userRepository.save(user);
  }

  @Transactional(readOnly = true)
  public UserModel getUser(String userId) {
    //Ask: userId도 UserModel 안에 넣어서 왔으면, userId Model에서 검증 가능
    if (userId == null || userId.isBlank()) {
      throw new CoreException(ErrorType.BAD_REQUEST, "ID가 없습니다.");
    }
    return userRepository.findByUserId(userId).orElse(null);
  }
}
