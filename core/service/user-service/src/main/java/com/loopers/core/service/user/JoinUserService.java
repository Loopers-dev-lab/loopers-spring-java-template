package com.loopers.core.service.user;

import com.loopers.core.domain.error.DomainErrorCode;
import com.loopers.core.domain.user.User;
import com.loopers.core.domain.user.UserPoint;
import com.loopers.core.domain.user.repository.UserPointRepository;
import com.loopers.core.domain.user.repository.UserRepository;
import com.loopers.core.domain.user.type.UserGender;
import com.loopers.core.domain.user.vo.UserBirthDay;
import com.loopers.core.domain.user.vo.UserEmail;
import com.loopers.core.domain.user.vo.UserIdentifier;
import com.loopers.core.service.user.command.JoinUserCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JoinUserService {

    private final UserRepository userRepository;
    private final UserPointRepository userPointRepository;

    @Transactional
    public User joinUser(JoinUserCommand command) {
        UserIdentifier userIdentifier = UserIdentifier.create(command.getUserIdentifier());
        boolean presentIdentifier = userRepository.findByIdentifier(userIdentifier)
                .isPresent();

        if (presentIdentifier) {
            throw new IllegalArgumentException(DomainErrorCode.PRESENT_USER_IDENTIFIER.getMessage());
        }

        UserEmail userEmail = UserEmail.create(command.getEmail());
        UserBirthDay userBirthDay = UserBirthDay.create(command.getBirthDay());
        UserGender userGender = UserGender.create(command.getGender());

        User savedUser = userRepository.save(User.create(userIdentifier, userEmail, userBirthDay, userGender));
        userPointRepository.save(UserPoint.create(savedUser.getId()));

        return savedUser;
    }
}
