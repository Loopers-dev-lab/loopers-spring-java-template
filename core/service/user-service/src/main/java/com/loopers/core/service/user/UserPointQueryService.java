package com.loopers.core.service.user;

import com.loopers.core.domain.error.NotFoundException;
import com.loopers.core.domain.user.User;
import com.loopers.core.domain.user.UserPoint;
import com.loopers.core.domain.user.repository.UserPointRepository;
import com.loopers.core.domain.user.repository.UserRepository;
import com.loopers.core.domain.user.vo.UserIdentifier;
import com.loopers.core.service.user.query.GetUserPointQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserPointQueryService {

    private final UserRepository userRepository;
    private final UserPointRepository userPointRepository;

    public UserPoint getByUserIdentifier(GetUserPointQuery query) {
        try {
            User user = userRepository.getByIdentifier(new UserIdentifier(query.getUserIdentifier()));
            
            return userPointRepository.getByUserId(user.getUserId());
        } catch (NotFoundException exception) {
            return null;
        }
    }
}
