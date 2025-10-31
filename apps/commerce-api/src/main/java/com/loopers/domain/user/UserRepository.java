package com.loopers.domain.user;

import java.util.Optional;

public interface UserRepository {
    /**
 * Persist the given User entity and return the persisted instance.
 *
 * @param user the User to persist
 * @return the persisted User instance, potentially with updated state (for example, an assigned identifier or updated audit fields)
 */
User save(User user);

    /**
 * Locates a user by its identifier.
 *
 * @param id the user's identifier
 * @return an Optional containing the User with the given id if found, otherwise an empty Optional
 */
Optional<User> findById(String id);

    /**
 * Checks whether a user with the given identifier exists in the repository.
 *
 * @param id the user identifier to check for existence
 * @return `true` if a user with the specified `id` exists, `false` otherwise
 */
boolean existsById(String id);
}