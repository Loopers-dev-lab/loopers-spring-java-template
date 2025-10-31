package com.loopers.infrastructure.user;

import com.loopers.domain.user.User;
import com.loopers.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@RequiredArgsConstructor
@Repository
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository userJpaRepository;

    /**
     * Persist the given user and return the persisted entity.
     *
     * @param user the user to persist; can be a new entity or an existing entity with updates
     * @return the persisted User entity
     */
    @Override
    public User save(User user) {
        return userJpaRepository.save(user);
    }

    /**
     * Retrieve a User by its identifier.
     *
     * @param id the user's identifier
     * @return an Optional containing the User if found, Optional.empty() otherwise
     */
    @Override
    public Optional<User> findById(String id) {
        return userJpaRepository.findById(id);
    }

    /**
     * Checks whether a user with the given id exists in the repository.
     *
     * @param id the user's identifier
     * @return true if a user with the given id exists, false otherwise
     */
    @Override
    public boolean existsById(String id) {
        return userJpaRepository.existsById(id);
    }
}