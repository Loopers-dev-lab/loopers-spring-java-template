package com.loopers.core.domain.user.repository;

import com.loopers.core.domain.user.User;
import com.loopers.core.domain.user.vo.UserId;

import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(UserId userId);
}
