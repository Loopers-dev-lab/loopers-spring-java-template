package com.loopers.domain.point;

import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class PointService {

    private final PointAccountRepository pointAccountRepository;
    private final UserRepository userRepository;
    private final UserFacade userFacade;

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

    @Transactional
    public Point charge(String userId, long amount) {

        UserInfo user = userFacade.getUser(userId);

        PointAccount account = pointAccountRepository.find(user.userId())
                .orElseGet(() -> pointAccountRepository.save(PointAccount.create(user.userId())));

        account.charge(amount);

        return account.getBalance();
    }
}
