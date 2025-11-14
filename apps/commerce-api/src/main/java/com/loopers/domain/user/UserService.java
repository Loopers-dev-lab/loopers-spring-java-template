package com.loopers.domain.user;

import com.loopers.application.user.UserInfo;
import com.loopers.domain.point.PointAccount;
import com.loopers.domain.point.PointAccountRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class UserService {

    private final UserRepository userRepository;
    private final PointAccountRepository pointAccountRepository;

    public UserInfo register(String id, String email, String birth, Gender gender) {
        if (userRepository.existsByUserId(id)) {
            throw new CoreException(ErrorType.CONFLICT, "중복된 ID 입니다.");
        }

        User user = userRepository.save(User.create(id, email, birth, gender));

        //포인트 계좌 생성 (0원으로 초기화)
        pointAccountRepository.save(PointAccount.create(user.getUserId()));

        return UserInfo.from(user);
    }

    @Transactional(readOnly = true)
    public User getUser(String userId) {
        return userRepository.find(userId)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public User findUser(String userId) {
        return userRepository.find(userId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND,
                        "해당 사용자를 찾을 수 없습니다."
                ));
    }
}
