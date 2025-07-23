package com.loopers.domain.user;

import com.loopers.domain.user.embeded.UserLoginId;

import java.util.Optional;

public interface UserRepository {
    //Optional을 안쓰는 이유는 save는 UserEntity가 보장이 되기 떄문이다.
    UserEntity save(UserEntity user);
    boolean existsById(Long id);
    boolean existsByLoginId(UserLoginId loginId);
    Optional<UserEntity> findById(Long id);
    void deleteAll();
}

