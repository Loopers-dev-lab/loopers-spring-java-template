package com.loopers.application.user;

import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class UserFacade {

    private final UserService userService;

    /**
     * Register a new user and return its UserInfo representation.
     *
     * @param id        the unique identifier for the user
     * @param email     the user's email address
     * @param birthDate the user's birth date (string)
     * @param gender    the user's gender
     * @return          the created user's UserInfo
     */
    public UserInfo registerUser(String id, String email, String birthDate, String gender) {
        User user = userService.registerUser(id, email, birthDate, gender);
        return UserInfo.from(user);
    }

    /**
     * Retrieve a user's information by their identifier.
     *
     * @param id the user's identifier
     * @return the UserInfo for the user with the given id, or `null` if no user is found
     */
    public UserInfo getUserById(String id) {
        User user = userService.getUserById(id);
        if (user == null) {
            return null;
        }
        return UserInfo.from(user);
    }
}