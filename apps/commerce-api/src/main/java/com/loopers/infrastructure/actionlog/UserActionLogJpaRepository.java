package com.loopers.infrastructure.actionlog;

import com.loopers.domain.actionlog.UserActionLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserActionLogJpaRepository extends JpaRepository<UserActionLog, Long> {
}
