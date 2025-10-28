
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
    return userRepository.save(user);
  }

  @Transactional(readOnly = true)
  public User getUser(Long id) {
    return userRepository.find(id)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "[id = " + id + "] 예시를 찾을 수 없습니다."));
  }
}
