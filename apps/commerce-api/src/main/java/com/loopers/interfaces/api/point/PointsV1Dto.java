package com.loopers.interfaces.api.point;

import com.loopers.domain.point.Point;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 포인트 API v1의 데이터 전송 객체(DTO) 컨테이너.
 *
 * @author Loopers
 * @version 1.0
 */
public class PointsV1Dto {
    /**
     * 포인트 정보 응답 데이터.
     *
     * @param userId 사용자 ID
     * @param balance 포인트 잔액
     */
    public record PointsResponse(String userId, Long balance) {
        /**
         * Point 엔티티로부터 PointsResponse를 생성합니다.
         *
         * @param point 포인트 엔티티
         * @return 생성된 응답 객체
         */
        public static PointsResponse from(Point point) {
            return new PointsResponse(point.getUser().getUserId(), point.getBalance());
        }
    }

    /**
     * 포인트 충전 요청 데이터.
     *
     * @param amount 충전할 포인트 금액 (필수, 0보다 커야 함)
     */
    public record ChargeRequest(
        @NotNull(message = "포인트는 필수입니다.")
        @Positive(message = "포인트는 0보다 큰 값이어야 합니다.")
        Long amount
    ) {}
}