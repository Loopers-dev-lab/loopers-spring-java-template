package com.loopers.application.user;

import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class UserFacade {

    private final UserService userService;
    private final PointService pointService;

    /**
     * 회원가입 (User 저장하기)
     * @param userInfo
     * @return
     */
    @Transactional
    public UserInfo saveUser(UserInfo userInfo) {

        // 이미 존재하는 회원 객체가 있는지 확인
        Optional<User> foundUser = userService.findUserByLoginId(userInfo.loginId());
        if(foundUser.isPresent()){
            throw new CoreException(ErrorType.CONFLICT, "이미 존재하는 회원을 다시 저장 못합니다.");
        }

        // 회원 객체 생성 (point는 null, 빈 likes 리스트 초기화)
        User user = User.builder()
                .loginId(userInfo.loginId())
                .email(userInfo.email())
                .birthday(userInfo.birthday())
                .gender(userInfo.gender())
                .build();

        // 회원 저장 (Point 없이 먼저 저장)
        User savedUser = userService.saveUser(user)
                .orElseThrow(() -> new CoreException(ErrorType.INTERNAL_ERROR, "User 를 저장하지 못했습니다."));

        // User 저장 후 Point 별도 생성
        Point point = Point.builder()
                .userId(savedUser.getId())
                .build();
        Point savedPoint = pointService.savePoint(point)
                .orElseThrow(() -> new CoreException(
                        ErrorType.INTERNAL_ERROR,
                        "Point 저장에 실패했습니다."
                ));

        return UserInfo.from(savedUser, savedPoint);
    }

    /**
     * 회원 조회 (User 정보 가져오기)
     * @param userId
     * @return
     */
    @Transactional(readOnly = true)
    public UserInfo getUser(Long userId){
        User user = userService.findById(userId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "[userId = " + userId + "] User 를 찾지 못했습니다."));
        
        // Point 별도 조회
        Point point = pointService.findByUserId(userId)
                .orElse(null); // Point가 없을 수도 있음
        
        return UserInfo.from(user, point);
    }
}
