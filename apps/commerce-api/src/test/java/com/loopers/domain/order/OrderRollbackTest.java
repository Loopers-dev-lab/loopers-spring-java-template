package com.loopers.domain.order;

import com.loopers.application.order.OrderFacade;
import com.loopers.application.order.OrderPlaceCommand;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponRepository;
import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.User;
import com.loopers.fixture.TestFixture;
import com.loopers.support.error.CoreException;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class OrderRollbackTest {

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private PointRepository pointRepository;

    @Autowired
    private TestFixture testFixture;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private User user;
    private Brand brand;
    private Product product1;
    private Product product2;  // 재고 0
    private Coupon coupon;
    private Point point;

    @BeforeEach
    void setUp() {
        databaseCleanUp.truncateAllTables();

        user = testFixture.createUser("rollbackUser");
        point = testFixture.createPoint(user.getId(), 10000L);

        brand = testFixture.createBrand("Test Brand");
        product1 = testFixture.createProduct("Product 1", 5000L, 10, brand);
        product2 = testFixture.createProduct("Product 2 (OutOfStock)", 1000L, 0, brand);

        coupon = testFixture.createFixedAmountCoupon(user, 1000L);
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("재고 부족 시, 사용하려던 쿠폰과 포인트가 롤백되어야 한다.")
    @Test
    void placeOrder_Rolls_Back_When_Stock_Is_Insufficient() {
        OrderPlaceCommand command = new OrderPlaceCommand(
                user.getLoginIdValue(),
                List.of(new OrderPlaceCommand.OrderItemCommand(product2.getId(), 1)),
                coupon.getId(),
                OrderPlaceCommand.PaymentMethod.POINT,
                null
        );

        assertThrows(CoreException.class, () -> {
            orderFacade.createOrder(command);
        });

        Coupon foundCoupon = couponRepository.findById(coupon.getId()).orElseThrow();
        assertThat(foundCoupon.getIsUsed()).isFalse();

        Point foundPoint = pointRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(foundPoint.getBalanceValue()).isEqualTo(10000L);
    }

    @DisplayName("포인트 부족 시, 사용하려던 쿠폰과 재고가 롤백되어야 한다.")
    @Test
    void placeOrder_Rolls_Back_When_Point_Is_Insufficient() {
        OrderPlaceCommand command = new OrderPlaceCommand(
                user.getLoginIdValue(),
                List.of(new OrderPlaceCommand.OrderItemCommand(product2.getId(), 1)),
                coupon.getId(),
                OrderPlaceCommand.PaymentMethod.POINT,
                null
        );

        assertThrows(CoreException.class, () -> {
            orderFacade.createOrder(command);
        });

        Product foundProduct = productRepository.findById(product1.getId()).orElseThrow();
        assertThat(foundProduct.getStockValue()).isEqualTo(10);

        Coupon foundCoupon = couponRepository.findById(coupon.getId()).orElseThrow();
        assertThat(foundCoupon.getIsUsed()).isFalse();
    }
}
