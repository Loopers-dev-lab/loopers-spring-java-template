package com.loopers.application;

import com.loopers.application.order.OrderCommand;
import com.loopers.application.order.OrderResult;
import com.loopers.application.order.UserOrderProductFacade;
import com.loopers.domain.brand.BrandModel;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.brand.BrandStatus;
import com.loopers.domain.product.ProductModel;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.ProductStatus;
import com.loopers.domain.user.UserModel;
import com.loopers.domain.user.UserRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
public class OrderFacadeTest {
    @Autowired
    private UserOrderProductFacade userOrderProductFacade;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    UserModel user;
    BrandModel brand;
    ProductModel product;

    private static final int INIT_POINT = 1_000;

    @BeforeEach
    void setUp() {
        user = new UserModel(
                "testuser1",
                "user2",
                "유저테스트",
                "a@b.com",
                "1997-09-28",
                "M",
                INIT_POINT);
        brand = new BrandModel(
                "테스트브랜드",
                "테스트브랜드입니다.",
                BrandStatus.REGISTERED);
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @Test
    @DisplayName("재고가 존재하지 않거나 부족한 경우 주문은 실패한다.")
    void placeOrderTest_throwsExceptionWhenNotEnoughStocks() {
        // given
        UserModel resultUser = userRepository.save(user);
        BrandModel resultBrand = brandRepository.save(brand);

        product = ProductModel.builder()
                .name("상품B")
                .category("테스트상품")
                .price(0)
                .stock(0)
                .status(ProductStatus.ON_SALE)
                .brand(resultBrand)
                .build();

        ProductModel resultProduct = productRepository.save(product);

        OrderCommand.OrderLine line = new OrderCommand.OrderLine(resultProduct.getId(), 1);
        OrderCommand.Order order = new OrderCommand.Order(
                user.getUserId(),
                List.of(line)
        );

        // when
        CoreException result =
                assertThrows(CoreException.class, () -> {
                    userOrderProductFacade.placeOrder(order);
                });

        assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
    }

    @Test
    @DisplayName("유저 포인트 부족시 주문은 실패한다.")
    void placeOrderTest_throwsExceptionWhenUserHasNotEnoughPoints() {
        // given
        UserModel resultUser = userRepository.save(user);
        BrandModel resultBrand = brandRepository.save(brand);

        product = ProductModel.builder()
                .name("상품B")
                .category("테스트상품")
                .price(20000)
                .stock(10)
                .status(ProductStatus.ON_SALE)
                .brand(resultBrand)
                .build();

        ProductModel resultProduct = productRepository.save(product);

        OrderCommand.OrderLine line = new OrderCommand.OrderLine(resultProduct.getId(), 1);
        OrderCommand.Order order = new OrderCommand.Order(
                resultUser.getUserId(),
                List.of(line)
        );

        // when
        CoreException result =
                assertThrows(CoreException.class, () -> {
                    userOrderProductFacade.placeOrder(order);
                });

        // then
        assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
    }

    @Test
    @DisplayName("주문 성공 시, 모든 처리는 정상 반영되어야 한다")
    void placeOrderTest_whenValidOrder() {
        // given
        UserModel resultUser = userRepository.save(user);
        BrandModel resultBrand = brandRepository.save(brand);

        int productPrice = 500;
        product = ProductModel.builder()
                .name("상품B")
                .category("테스트상품")
                .price(productPrice)
                .stock(10)
                .status(ProductStatus.ON_SALE)
                .brand(resultBrand)
                .build();

        ProductModel resultProduct = productRepository.save(product);

        OrderCommand.OrderLine line = new OrderCommand.OrderLine(resultProduct.getId(), 1);
        OrderCommand.Order order = new OrderCommand.Order(
                resultUser.getUserId(),
                List.of(line)
        );

        // when
        OrderResult.PlaceOrderResult result = userOrderProductFacade.placeOrder(order);

        // then
        assertThat(result.orderId()).isNotNull();
        assertThat(result.normalPrice()).isEqualTo(productPrice);
        assertThat(result.successLines().isEmpty()).isFalse();
        assertThat(result.failedLines().isEmpty()).isTrue();
    }

}
