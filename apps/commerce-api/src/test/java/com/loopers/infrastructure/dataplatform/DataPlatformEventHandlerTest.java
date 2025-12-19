package com.loopers.infrastructure.dataplatform;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.event.OrderCompletedEvent;
import com.loopers.fixture.OrderFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataPlatformEventHandlerTest {

    @Mock
    private DataPlatform dataPlatform;

    @InjectMocks
    private DataPlatformEventHandler eventHandler;

    @Test
    @DisplayName("주문 완료 이벤트를 받으면 데이터 플랫폼에 전송한다")
    void dataPlatformTest1() {
        // arrange
        Order order = OrderFixture.create("user-1", 10000L, 1000L);
        OrderCompletedEvent event = OrderCompletedEvent.from(order);

        // act
        eventHandler.handleOrderCompleted(event);

        // assert
        verify(dataPlatform, times(1)).send(anyString());
    }

    @Test
    @DisplayName("데이터 플랫폼에서 예외가 발생해도 이벤트 핸들러는 예외를 던지지 않는다")
    void dataPlatformTest2() {
        // arrange
        Order order = OrderFixture.create("user-1", 10000L, 1000L);
        OrderCompletedEvent event = OrderCompletedEvent.from(order);
        when(dataPlatform.send(anyString())).thenThrow(new RuntimeException("Network error"));

        // act & assert - 예외 없이 정상 종료
        eventHandler.handleOrderCompleted(event);

        verify(dataPlatform, times(1)).send(anyString());
    }
}
