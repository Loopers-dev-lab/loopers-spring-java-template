
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
  public User join(User user) {
    if (userRepository.findByLoginId(user.getLoginId()).isPresent()) {
      throw new CoreException(ErrorType.BAD_REQUEST, "이미 가입된 ID 입니다.");
    }
    return userRepository.save(user);
  }

  @Transactional(readOnly = true)
  public User getUser(Long userId) {
    if (userId == null) {
      throw new CoreException(ErrorType.BAD_REQUEST, "ID가 없습니다.");
    }
    return userRepository.findById(userId).orElse(null);
  }

  @Transactional(readOnly = true)
  public User getUser(String loginId) {
    if (loginId == null) {
      throw new CoreException(ErrorType.BAD_REQUEST, "ID가 없습니다.");
    }
    return userRepository.findByLoginId(loginId).orElse(null);
  }

  @Transactional(readOnly = true)
  public User getActiveUser(Long userId) {
    if (userId == null) {
      throw new CoreException(ErrorType.BAD_REQUEST, "ID가 없습니다.");
    }
    return userRepository.findById(userId)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "사용자 정보를 찾을 수 없습니다."));
  }
}
