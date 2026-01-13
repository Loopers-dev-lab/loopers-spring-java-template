package com.loopers.domain.point;

import com.loopers.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Component;

import com.loopers.domain.user.UserModel;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

@RequiredArgsConstructor
@Component
public class PointService {

    private final PointRepository pointRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PointModel findPoint(PointModel point) {
        UserModel requestUser = point.getUser();
        var foundUser = userRepository.find(requestUser.getUserId());
        if (foundUser.isEmpty()) {
            return null;
        }
        return pointRepository.findPoint(foundUser.get()).orElse(null);
    }
  
    @Transactional
    public void charge(PointModel point) {
        UserModel user = point.getUser();
        var foundUser = userRepository.find(user.getUserId())
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "유저가 존재하지 않습니다."));
  
        var existing = pointRepository.findPoint(foundUser);
        if (existing.isPresent()) {
            existing.get().charge(point.getPoint());
            pointRepository.save(existing.get());
            return;
        }
        pointRepository.save(new PointModel(foundUser, point.getPoint()));
    }
}
