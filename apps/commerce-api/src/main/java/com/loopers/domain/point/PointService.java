package com.loopers.domain.point;

import com.loopers.domain.user.UserRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
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

    @Transactional
    public Point charge(String userId, long amount) {

        PointAccount account = pointAccountRepository.find(userId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 유저 입니다."));

        account.charge(amount);

        return account.getBalance();
    }
}
