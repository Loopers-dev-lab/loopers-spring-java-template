package com.loopers.domain.user;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceUnitUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class UserLazyTest {
  @Autowired
  private UserRepository userRepository;

  @Autowired
  private EntityManager entityManager;

  /**
   * 테스트 유틸리티: User 엔티티 생성 및 저장
   */
  private User setupUserWithPoint() {
    // 픽스처 대신 직접 생성하여 명확하게 Point가 생성됨을 보장
    User user = User.create("testuser1", "test@email.com", "1990-01-01", "M");

    // Point.create 내부에서 Point 객체가 생성되고 user.setPoint(this)를 통해 양방향 연결됨
    // User 생성자 내부에 'this.point = Point.create(this, BigDecimal.TEN);' 로직이 있으므로 별도 setPoint 불필요

    return userRepository.save(user);
  }

  @Test
  @Transactional // 트랜잭션 내에서 조회해야 영속성 컨텍스트를 사용할 수 있습니다.
  @DisplayName("User 조회 시 Point 필드는 로드되지 않아야 한다 (FetchType.LAZY 검증)")
  void testPointIsLazyLoaded() {
    // 1. Arrange: User 및 Point 데이터 저장
    User savedUser = setupUserWithPoint();

    // 2. Clear: 영속성 컨텍스트를 비워, 다음 findById가 DB에서 새로운 엔티티를 로드하도록 강제
    // (이것이 없으면 1차 캐시의 User를 재사용할 수 있음)
    entityManager.clear();

    // 3. Act: DB에서 User 엔티티를 다시 조회
    // 이 시점에 Point는 LAZY이므로 로드되면 안 됩니다.
    User foundUser = userRepository.findById(savedUser.getId())
        .orElseThrow(() -> new AssertionError("User Not Found"));

    // 4. Assert: PersistenceUnitUtil을 사용하여 Point 필드의 로드 여부 검증
    // PersistenceUnitUtil은 필드에 접근하지 않고 로딩 상태만 확인합니다.
    PersistenceUnitUtil unitUtil = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();

    boolean isPointLoaded = unitUtil.isLoaded(foundUser, "point");

    // 검증: LAZY 설정대로 Point 필드가 로드되지 않았어야 합니다.
    assertThat(isPointLoaded)
        .as("User 엔티티를 조회할 때 Point 필드는 LAZY Loading으로 인해 로드되지 않아야 합니다.")
        .isFalse();
  }
}
