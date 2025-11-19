package com.loopers.domain.point;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PointAccountDomainService {

    private final PointAccountRepository pointAccountRepository;

    /**
     * 포인트 계좌 생성
     *
     * @param userId
     * @return
     */
    @Transactional
    public PointAccount createForUser(String userId) {
        PointAccount account = PointAccount.create(userId);
        return pointAccountRepository.save(account);
    }

    /**
     * 포인트 조회
     */
    @Transactional(readOnly = true)
    public Point getBalance(String userId) {

        return pointAccountRepository.find(userId)
                .map(PointAccount::getBalance)
                .orElse(null);
    }

    /**
     * 포인트 충전
     */
    @Transactional
    public Point charge(String userId, long amount) {

        PointAccount account = pointAccountRepository.find(userId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 유저 입니다."));

        account.charge(amount);

        return account.getBalance();
    }


    /**
     * 포인트 차감
     */
    @Transactional
    public Point deduct(String userId, long amount) {
        PointAccount account = pointAccountRepository.find(userId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 유저 입니다."));

        account.deduct(amount);

        return account.getBalance();
    }
}
