package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

  private final UserRepository userRepository;

  @Transactional
  public User registerUser(String userId, String email, LocalDate birth) {
    userRepository.findByUserId(userId).ifPresent(user -> {
      throw new CoreException(ErrorType.CONFLICT, "이미 존재하는 사용자 ID입니다.");
    });

    User user = new User(userId, email, birth);
    return userRepository.save(user);
  }
}
