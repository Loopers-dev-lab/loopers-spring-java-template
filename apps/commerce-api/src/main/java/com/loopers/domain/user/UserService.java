package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

  private final UserRepository userRepository;

  @Transactional
  public User registerUser(String loginId, String email, LocalDate birth, Gender gender) {
    try {
      return userRepository.save(User.of(loginId, email, birth, gender));
    } catch (DataIntegrityViolationException e) {
      throw new CoreException(ErrorType.CONFLICT, "이미 존재하는 로그인 ID입니다.");
    }
  }

  public User findById(String loginId) {
    return userRepository.findByLoginId(loginId).orElse(null);
  }
}
