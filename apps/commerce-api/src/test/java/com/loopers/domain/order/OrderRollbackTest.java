package com.loopers.domain.order;

import com.loopers.application.order.OrderFacade;
import com.loopers.application.order.OrderPlaceCommand;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponRepository;
import com.loopers.domain.coupon.DiscountType;
import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
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
    private UserService userService;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private PointRepository pointRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private User user;
    private Brand brand;
    private Product product1;
    private Product product2;
    private Coupon coupon;
    private Point point;

    @BeforeEach
    void setUp() {
        databaseCleanUp.truncateAllTables();

        user = userService.signUp("testUser", "test@mail.com", "1990-01-01", Gender.MALE);
        point = pointRepository.save(Point.create(user.getUserIdValue(), 10000L));

        brand = brandRepository.save(Brand.create("Test Brand"));
        product1 = productRepository.save(Product.create("Product 1", 5000L, 10, brand));
        product2 = productRepository.save(Product.create("Product 2", 1000L, 0, brand));

        coupon = couponRepository.save(Coupon.create(user, "1000원 할인", DiscountType.FIXED_AMOUNT, 1000L));
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("재고 부족 시, 사용하려던 쿠폰과 포인트가 롤백되어야 한다.")
    @Test
    void placeOrder_Rolls_Back_When_Stock_Is_Insufficient() {
        // arrange
        OrderPlaceCommand command = new OrderPlaceCommand(
                user.getUserIdValue(),
                List.of(new OrderPlaceCommand.OrderItemCommand(product2.getId(), 1)),
                coupon.getId()
        );

        // act & assert
        assertThrows(CoreException.class, () -> {
            orderFacade.placeOrder(command);
        });

        Coupon foundCoupon = couponRepository.findById(coupon.getId()).orElseThrow();
        assertThat(foundCoupon.getIsUsed()).isFalse();

        Point foundPoint = pointRepository.findByUserId(user.getUserIdValue()).orElseThrow();
        assertThat(foundPoint.getBalanceValue()).isEqualTo(10000L);
    }

    @DisplayName("포인트 부족 시, 사용하려던 쿠폰과 재고가 롤백되어야 한다.")
    @Test
    void placeOrder_Rolls_Back_When_Point_Is_Insufficient() {
        // arrange
        OrderPlaceCommand command = new OrderPlaceCommand(
                user.getUserIdValue(),
                List.of(new OrderPlaceCommand.OrderItemCommand(product1.getId(), 3)),
                coupon.getId()
        );

        // act & assert
        assertThrows(CoreException.class, () -> {
            orderFacade.placeOrder(command);
        });

        Product foundProduct = productRepository.findById(product1.getId()).orElseThrow();
        assertThat(foundProduct.getStockValue()).isEqualTo(10);

        Coupon foundCoupon = couponRepository.findById(coupon.getId()).orElseThrow();
        assertThat(foundCoupon.getIsUsed()).isFalse();
    }
}
