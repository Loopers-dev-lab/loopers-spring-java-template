package com.loopers.domain.user;

public interface UserRepository {
    User save(User user);
    User findByUserId(String userId);
}
