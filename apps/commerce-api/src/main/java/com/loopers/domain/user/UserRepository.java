package com.loopers.domain.user;

import java.util.Optional;

public interface UserRepository {
    Optional<UserModel> find(Long id);
}
