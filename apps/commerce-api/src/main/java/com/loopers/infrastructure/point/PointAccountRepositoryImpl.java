package com.loopers.infrastructure.point;

import com.loopers.domain.point.PointAccount;
import com.loopers.domain.point.PointAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class PointAccountRepositoryImpl implements PointAccountRepository {

    private final PointAccountJpaRepository pointAccountJpaRepository;

    @Override
    public Optional<PointAccount> find(String id) {
        return pointAccountJpaRepository.findByUserId(id);
    }

    @Override
    public PointAccount save(PointAccount pointAccount) {
        return pointAccountJpaRepository.save(pointAccount);
    }
}
