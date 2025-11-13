package com.loopers.domain.points;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@RequiredArgsConstructor
@Component
public class PointService {

    private final PointRepository pointRepository;

    @Transactional(readOnly = true)
    public BigDecimal getMemberPoints(String memberId) {
        return pointRepository.findByMemberId(memberId)
                .map(PointModel::getAmount)
                .orElse(null);
    }

    @Transactional
    public PointModel initializeMemberPoints(String memberId) {
        PointModel point = PointModel.create(memberId, BigDecimal.ZERO);
        return pointRepository.save(point);
    }
}
