package com.loopers.infrastructure.user;

import com.loopers.domain.user.User;
import com.loopers.domain.user.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository repository;

    @Override
    public User save(final User model) {
        return repository.save(model);
    }

    @Override
    public boolean existsByUserId(final String userId) {
        return repository.existsByUserId(userId);
    }

    @Override
    public Optional<User> find(final Long id) {
        return repository.findById(id);
    }
}
