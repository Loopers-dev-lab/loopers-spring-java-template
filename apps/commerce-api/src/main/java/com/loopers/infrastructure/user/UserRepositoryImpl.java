package com.loopers.infrastructure.user;

import com.loopers.domain.user.User;
import com.loopers.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * UserRepository의 JPA 구현체.
 * <p>
 * Spring Data JPA를 활용하여 User 엔티티의 
 * 영속성 작업을 처리합니다.
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
@RequiredArgsConstructor
@Component
public class UserRepositoryImpl implements UserRepository {
    private final UserJpaRepository userJpaRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    public User save(User user) {
        return userJpaRepository.save(user);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public User findByUserId(String userId) {
        return userJpaRepository.findByUserId(userId).orElse(null);
    }

    /**
     * Retrieve a User by its userId using database-level "for update" (row lock) semantics.
     *
     * @param userId the unique identifier of the user to retrieve
     * @return the User with the given userId, or null if no matching user is found
     */
    @Override
    public User findByUserIdForUpdate(String userId) {
        return userJpaRepository.findByUserIdForUpdate(userId).orElse(null);
    }
}