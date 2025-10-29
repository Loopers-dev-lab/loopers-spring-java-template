
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
  public UserModel join(UserModel user) {
    if (userRepository.existsByUserId(user.getUserId())) {
      throw new CoreException(ErrorType.BAD_REQUEST, "이미 가입된 ID 입니다.");
    }
    return userRepository.save(user);
  }

  @Transactional(readOnly = true)
  public UserModel getUser(String userId) {
    UserModel userModel = userRepository.findByUserId(userId).orElse(null);
    if (userModel == null) {
      throw new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 유저입니다.");
    }
    return userModel;
  }
}
