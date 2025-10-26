package com.loopers.core.domain.user.repository;

import com.loopers.core.domain.user.User;

public interface UserRepository {
    
    User save(User user);
}
