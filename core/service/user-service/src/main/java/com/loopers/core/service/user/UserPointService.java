package com.loopers.core.service.user;

import com.loopers.core.domain.user.User;
import com.loopers.core.domain.user.UserPoint;
import com.loopers.core.domain.user.repository.UserPointRepository;
import com.loopers.core.domain.user.repository.UserRepository;
import com.loopers.core.domain.user.vo.UserIdentifier;
import com.loopers.core.service.user.command.UserPointChargeCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserPointService {

    private final UserRepository userRepository;
    private final UserPointRepository userPointRepository;

    @Transactional
    public UserPoint charge(UserPointChargeCommand command) {
        User user = userRepository.getByIdentifierWithLock(new UserIdentifier(command.getUserIdentifier()));
        UserPoint userPoint = userPointRepository.getByUserId(user.getUserId());

        return userPointRepository.save(userPoint.charge(command.getPoint()));
    }
}
