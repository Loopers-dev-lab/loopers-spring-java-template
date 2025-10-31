package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class UserService {

    private final UserRepository userRepository;

    /**
     * Register a new user and persist it to the repository.
     *
     * @param id        the unique identifier for the user
     * @param email     the user's email address
     * @param birthDate the user's birth date as a string
     * @param gender    the user's gender value
     * @return          the persisted {@code User} instance
     * @throws CoreException if a user with the given {@code id} already exists (ErrorType.CONFLICT)
     */
    @Transactional
    public User registerUser(String id, String email, String birthDate, String gender) {
        if (userRepository.existsById(id)) {
            throw new CoreException(ErrorType.CONFLICT, "이미 존재하는 ID입니다: " + id);
        }

        User user = User.create(id, email, birthDate, gender);
        return userRepository.save(user);
    }

    /**
     * Retrieve a user by its identifier.
     *
     * @param id the unique identifier of the user
     * @return the User with the given id, or null if no user exists with that id
     */
    @Transactional(readOnly = true)
    public User getUserById(String id) {
        return userRepository.findById(id).orElse(null);
    }
}