package com.loopers.application.point;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class PointLockFacade {

    private static final int MAX_RETRY_ATTEMPTS = 5;

    private final PointFacade pointFacade;

    public PointInfo chargePoint(PointCommand.ChargeCommand chargeCommand) {
        for (int retryCount = 0; retryCount < MAX_RETRY_ATTEMPTS; retryCount++) {
            try {
                return pointFacade.chargePoint(chargeCommand);
            } catch (Exception e) {
                if (retryCount == MAX_RETRY_ATTEMPTS - 1) {
                    throw e;
                }
                try {
                    Thread.sleep(10 * (retryCount + 1));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("포인트 충전 중 오류가 발생했습니다.");
                }
            }
        }
        throw new RuntimeException("포인트 충전에 실패했습니다.");
    }

}
