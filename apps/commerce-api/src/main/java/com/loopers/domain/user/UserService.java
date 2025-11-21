package com.loopers.domain.user;

import com.loopers.domain.Money;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@RequiredArgsConstructor
@Component
public class UserService {

    private final UserRepository userRepository;


    @Transactional
    public User accountUser(String userId, String email, String birthdate, Gender gender) {

        if( userRepository.existsByUserId(userId) ) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이미 존재하는 ID 입니다.");
        }

        User user = User.createUser(userId, email, birthdate, gender);

        return userRepository.save(user);
    }

    @Transactional( readOnly = true )
    public User getUserByUserId(String userId) {
        return userRepository.findUserByUserId(userId)
                .orElse(null);
    }

    @Transactional( readOnly = true )
    public User getUserPointByUserId(String userId) {
        return userRepository.findUserByUserId(userId)
                .orElse(null);
    }

    @Transactional
    public User chargePointByUserId(String notExistsUserId, BigDecimal chargePoint) {

        User findUser = userRepository.findUserByUserId(notExistsUserId)
                .orElseThrow(
                        () -> new CoreException(ErrorType.NOT_FOUND, "해당 ID 의 회원이 존재하지 않아 포인트 충전이 실패하였습니다.")
                );

        findUser.chargePoint(Money.of(chargePoint));

        return findUser;
    }
}
