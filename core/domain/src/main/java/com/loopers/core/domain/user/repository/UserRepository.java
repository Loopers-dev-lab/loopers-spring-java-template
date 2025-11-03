package com.loopers.core.domain.user.repository;

import com.loopers.core.domain.user.User;
import com.loopers.core.domain.user.vo.UserId;
import com.loopers.core.domain.user.vo.UserIdentifier;

import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(UserId userId);

    Optional<User> findByIdentifier(UserIdentifier identifier);

    User getByIdentifier(UserIdentifier identifier);
}
