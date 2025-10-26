package com.loopers.core.service.user;

import com.loopers.core.domain.user.User;
import com.loopers.core.domain.user.repository.UserRepository;
import com.loopers.core.domain.user.type.UserGender;
import com.loopers.core.domain.user.vo.UserBirthDay;
import com.loopers.core.domain.user.vo.UserEmail;
import com.loopers.core.domain.user.vo.UserIdentifier;
import com.loopers.core.service.user.command.CreateUserCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JoinUserService {

    private final UserRepository userRepository;

    public User createUser(CreateUserCommand command) {
        UserIdentifier userIdentifier = new UserIdentifier(command.getUserIdentifier());
        UserEmail userEmail = new UserEmail(command.getEmail());
        UserBirthDay userBirthDay = new UserBirthDay(command.getBirthDay());
        UserGender userGender = UserGender.valueOf(command.getGender());

        return userRepository.save(User.create(userIdentifier, userEmail, userBirthDay, userGender));
    }
}
