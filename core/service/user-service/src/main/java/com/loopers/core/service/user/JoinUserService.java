package com.loopers.core.service.user;

import com.loopers.core.domain.user.User;
import com.loopers.core.domain.user.repository.UserRepository;
import com.loopers.core.domain.user.type.UserGender;
import com.loopers.core.domain.user.vo.UserBirthDay;
import com.loopers.core.domain.user.vo.UserEmail;
import com.loopers.core.domain.user.vo.UserIdentifier;
import com.loopers.core.service.user.command.JoinUserCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JoinUserService {

    private final UserRepository userRepository;

    public User joinUser(JoinUserCommand command) {
        UserIdentifier userIdentifier = UserIdentifier.create(command.getUserIdentifier());
        UserEmail userEmail = UserEmail.create(command.getEmail());
        UserBirthDay userBirthDay = UserBirthDay.create(command.getBirthDay());
        UserGender userGender = UserGender.create(command.getGender());

        return userRepository.save(User.create(userIdentifier, userEmail, userBirthDay, userGender));
    }
}
