package com.loopers.domain.user;

import java.util.Optional;

/**
 * @author hyunjikoh
 * @since 2025. 10. 27.
 */
public interface UserRepository {
    /**
 * Persist the given user entity.
 *
 * The returned instance reflects any changes applied during persistence (for example generated identifiers or updated timestamps).
 *
 * @param userEntity the user entity to persist
 * @return the persisted UserEntity with any generated or updated fields populated
 */
UserEntity save(UserEntity userEntity);

    /**
 * Retrieves the user with the specified username.
 *
 * @param username the user's unique username
 * @return an Optional containing the matching UserEntity if found, `Optional.empty()` otherwise
 */
Optional<UserEntity> findByUsername(String username);

    /**
 * Retrieves a user by username and acquires a lock to prevent concurrent modifications.
 *
 * @param username the unique username of the user to retrieve
 * @return an Optional containing the locked UserEntity if found, otherwise an empty Optional
 */
Optional<UserEntity> findByUsernameWithLock(String username);
}