package com.loopers.domain.points;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@RequiredArgsConstructor
@Component
public class PointService {

    private final PointRepository pointRepository;

    /**
     * Retrieve the current point balance for the specified member.
     *
     * @param memberId the unique identifier of the member whose points to retrieve
     * @return the member's point amount as a BigDecimal, or `null` if the member has no point record
     */
    @Transactional(readOnly = true)
    public BigDecimal getMemberPoints(String memberId) {
        return pointRepository.findByMemberId(memberId)
                .map(PointModel::getAmount)
                .orElse(null);
    }

    /**
     * Creates and persists a PointModel for the given member with an initial amount of zero.
     *
     * @param memberId the identifier of the member to initialize points for
     * @return the persisted PointModel with amount set to zero
     */
    @Transactional
    public PointModel initializeMemberPoints(String memberId) {
        PointModel point = PointModel.create(memberId, BigDecimal.ZERO);
        return pointRepository.save(point);
    }
}