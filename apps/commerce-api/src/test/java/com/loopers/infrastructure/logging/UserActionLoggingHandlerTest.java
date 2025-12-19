package com.loopers.infrastructure.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.loopers.domain.coupon.event.CouponUsedEvent;
import com.loopers.domain.like.event.ProductLikedEvent;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.event.OrderCompletedEvent;
import com.loopers.fixture.OrderFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

class UserActionLoggingHandlerTest {

    private UserActionLoggingHandler handler;
    private ListAppender<ILoggingEvent> listAppender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        handler = new UserActionLoggingHandler();

        logger = (Logger) LoggerFactory.getLogger(UserActionLoggingHandler.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(listAppender);
    }

    @Test
    @DisplayName("주문 완료 이벤트를 받으면 ORDER_COMPLETED 로그를 남긴다")
    void handleOrderCompletedTest1() {
        // arrange
        Order order = OrderFixture.create("user-1", 10000L, 1000L);
        OrderCompletedEvent event = OrderCompletedEvent.from(order);

        // act
        handler.handleOrderCompleted(event);

        // assert
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent logEvent = listAppender.list.get(0);
        assertThat(logEvent.getLevel()).isEqualTo(Level.INFO);
        assertThat(logEvent.getFormattedMessage()).contains("[USER_ACTION]");
        assertThat(logEvent.getFormattedMessage()).contains("ORDER_COMPLETED");
        assertThat(logEvent.getFormattedMessage()).contains("userId=user-1");
    }

    @Test
    @DisplayName("좋아요 이벤트를 받으면 PRODUCT_LIKED 로그를 남긴다")
    void handleProductLikedTest1() {
        // arrange
        ProductLikedEvent event = ProductLikedEvent.liked(1L, 100L);

        // act
        handler.handleProductLiked(event);

        // assert
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent logEvent = listAppender.list.get(0);
        assertThat(logEvent.getLevel()).isEqualTo(Level.INFO);
        assertThat(logEvent.getFormattedMessage()).contains("[USER_ACTION]");
        assertThat(logEvent.getFormattedMessage()).contains("PRODUCT_LIKED");
        assertThat(logEvent.getFormattedMessage()).contains("productId=1");
    }

    @Test
    @DisplayName("좋아요 취소 이벤트를 받으면 PRODUCT_UNLIKED 로그를 남긴다")
    void handleProductLikedTest2() {
        // arrange
        ProductLikedEvent event = ProductLikedEvent.unliked(1L, 100L);

        // act
        handler.handleProductLiked(event);

        // assert
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent logEvent = listAppender.list.get(0);
        assertThat(logEvent.getFormattedMessage()).contains("PRODUCT_UNLIKED");
    }

    @Test
    @DisplayName("쿠폰 사용 이벤트를 받으면 COUPON_USED 로그를 남긴다")
    void handleCouponUsedTest1() {
        // arrange
        CouponUsedEvent event = CouponUsedEvent.from(1L, 100L, "user-1", 5000L);

        // act
        handler.handleCouponUsed(event);

        // assert
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent logEvent = listAppender.list.get(0);
        assertThat(logEvent.getLevel()).isEqualTo(Level.INFO);
        assertThat(logEvent.getFormattedMessage()).contains("[USER_ACTION]");
        assertThat(logEvent.getFormattedMessage()).contains("COUPON_USED");
        assertThat(logEvent.getFormattedMessage()).contains("couponId=1");
        assertThat(logEvent.getFormattedMessage()).contains("discountAmount=5000");
    }
}
