package com.loopers.domain.point;

import com.loopers.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class PointService {

    private final PointAccountRepository pointAccountRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Point getBalance(String userId) {
        boolean userExists = userRepository.existsByUserId(userId);

        if (!userExists) {
            return null;
        }

        return pointAccountRepository.find(userId)
                .map(PointAccount::getBalance)
                .orElse(Point.zero());
    }

}
