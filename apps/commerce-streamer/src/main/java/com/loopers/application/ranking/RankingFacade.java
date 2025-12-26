package com.loopers.application.ranking;


import com.loopers.domain.ranking.RankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingFacade {

    private final RankingService rankingService;
    private final Clock clock;

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    /**
     * 조회 이벤트 처리
     */
    public void processViewEvent(Long productId) {
        LocalDate date = today();
        rankingService.incrementViewScore(productId, date);
        log.debug("조회 이벤트 랭킹 반영: productId={}, date={}", productId, date);
    }

    /**
     * 좋아요 이벤트 처리
     */
    public void processLikeEvent(Long productId, boolean isLike) {
        LocalDate date = today();
        rankingService.updateLikeScore(productId, isLike, date);
        log.debug("좋아요 이벤트 랭킹 반영: productId={}, isLike={}, date={}", productId, isLike, date);
    }

    /**
     * 주문 이벤트 처리 (수량 기반)
     */
    public void processOrderEvent(Long productId, int quantity) {
        LocalDate date = today();
        rankingService.incrementOrderScore(productId, quantity, date);
        log.debug("주문 이벤트 랭킹 반영: productId={}, quantity={}, date={}", productId, quantity, date);
    }

    /**
     * 주문 이벤트 처리 (금액 기반)
     */
    public void processOrderEventWithAmount(Long productId, long amount) {
        LocalDate date = today();
        rankingService.incrementOrderScoreWithAmount(productId, amount, date);
        log.debug("주문 이벤트 랭킹 반영 (금액): productId={}, amount={}, date={}", productId, amount, date);
    }
}
