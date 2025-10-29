package com.loopers.domain.user;

import com.loopers.domain.example.ExampleModel;

import java.util.Optional;

public interface UserRepository {
    boolean existsByUserId(String id);
    void save(UserModel userModel);
}
