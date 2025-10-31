package com.loopers.core.service.user;

import com.loopers.core.domain.user.User;
import com.loopers.core.domain.user.repository.UserRepository;
import com.loopers.core.domain.user.vo.UserIdentifier;
import com.loopers.core.service.user.query.GetUserQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserQueryService {

    private final UserRepository userRepository;

    public User getUserByIdentifier(GetUserQuery query) {
        return userRepository.findByIdentifier(UserIdentifier.create(query.getIdentifier()))
                .orElse(null);
    }
}
