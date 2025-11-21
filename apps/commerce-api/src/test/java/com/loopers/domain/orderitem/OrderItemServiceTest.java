package com.loopers.domain.orderitem;

import com.loopers.domain.Money;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.order.Order;
import com.loopers.domain.product.Product;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.utils.DatabaseCleanUp;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@ActiveProfiles("test")
@SpringBootTest
class OrderItemServiceTest {

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("특정 주문의 주문 상품 목록을 조회한다.")
    @Test
    @Transactional
    void orderItemsByOrder() {
        // given
        Brand brand = Brand.builder()
                .brandName("테스트브랜드")
                .build();
        entityManager.persist(brand);

        User user = User.builder()
                .userId("testuser1")
                .email("test@example.com")
                .birthdate("1990-01-01")
                .gender(Gender.MALE)
                .build();
        user.chargePoint(Money.of(100000)); // 포인트 충전
        entityManager.persist(user);

        Product product1 = Product.builder()
                .productCode("PROD001")
                .productName("상품1")
                .price(Money.of(10000))
                .stockQuantity(100)
                .brand(brand)
                .build();
        Product product2 = Product.builder()
                .productCode("PROD002")
                .productName("상품2")
                .price(Money.of(20000))
                .stockQuantity(100)
                .brand(brand)
                .build();
        entityManager.persist(product1);
        entityManager.persist(product2);

        Map<Product, Integer> productQuantities = new HashMap<>();
        productQuantities.put(product1, 2);
        productQuantities.put(product2, 1);

        Order order = Order.createOrder(user, productQuantities);
        entityManager.persist(order);
        entityManager.flush();
        entityManager.clear();

        // when
        List<OrderItem> orderItems = orderItemService.getOrderItemsByOrder(order.getId());

        // then
        assertAll(
                () -> assertThat(orderItems).hasSize(2),
                () -> assertThat(orderItems).extracting("product.productName")
                        .containsExactlyInAnyOrder("상품1", "상품2"),
                () -> assertThat(orderItems).extracting("quantity")
                        .containsExactlyInAnyOrder(2, 1),
                () -> assertThat(orderItems).allMatch(item -> item.getOrder().getId().equals(order.getId()))
        );
    }
}
