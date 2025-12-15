package com.loopers.infrastructure.platform;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Getter
@Component
@Profile("test")
public class MockDataPlatformSender implements DataPlatformSender {

    private final List<OrderResultMessage> sentOrderMessages = new ArrayList<>();
    private final List<PaymentResultMessage> sentPaymentMessages = new ArrayList<>();
    private final List<UserActionMessage> sentUserActionMessages = new ArrayList<>();

    @Override
    public void sendOrderResult(OrderResultMessage message) {
        simulateNetworkDelay();
        sentOrderMessages.add(message);
        log.info("[MOCK DataPlatform] 주문 결과 전송: orderId={}, action={}, loginId={}",
                message.orderId(), message.action(), message.userId());
    }

    @Override
    public void sendPaymentResult(PaymentResultMessage message) {
        simulateNetworkDelay();
        sentPaymentMessages.add(message);
        log.info("[MOCK DataPlatform] 결제 결과 전송: paymentId={}, action={}, orderId={}",
                message.paymentId(), message.action(), message.orderId());
    }

    @Override
    public void sendUserAction(UserActionMessage message) {
        simulateNetworkDelay();
        sentUserActionMessages.add(message);
        log.info("[MOCK DataPlatform] 유저 행동 전송: loginId={}, action={}, targetId={}",
                message.userId(), message.actionType(), message.targetId());
    }

    private void simulateNetworkDelay() {
        try {
            Thread.sleep(500); // 0.5초 지연
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // 테스트용 초기화 메서드
    public void clear() {
        sentOrderMessages.clear();
        sentPaymentMessages.clear();
        sentUserActionMessages.clear();
    }
}
