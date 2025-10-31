package com.loopers.application.point;

import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class PointFacade {

    private final PointService pointService;

    /**
     * Retrieve the point information for the specified user.
     *
     * @param userId the identifier of the user whose points to retrieve
     * @return a PointInfo for the user, or {@code null} if no point record exists
     */
    public PointInfo getPointByUserId(String userId) {
        Point point = pointService.getPointByUserId(userId);
        if (point == null) {
            return null;
        }
        return PointInfo.from(point);
    }

    /**
     * Charges the specified user's points by the given amount and returns the updated point information.
     *
     * @param userId the identifier of the user whose points will be charged
     * @param amount the amount to charge to the user's points
     * @return the updated PointInfo reflecting the user's current points after the charge
     */
    public PointInfo charge(String userId, Long amount) {
        Point charged = pointService.charge(userId, amount);
        return PointInfo.from(charged);
    }
}