package com.loopers.domain.order;

import com.loopers.domain.Money;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.Stock;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserRepository;
import com.loopers.utils.DatabaseCleanUp;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@ActiveProfiles("test")
@SpringBootTest
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private EntityManager entityManager;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("주문 생성 시 상품의 재고를 차감한다.")
    @Test
    @Transactional
    void createOrder_decreaseStock_success() {
        // given
        User user = User.createUser("testUser", "test@test.com", "1990-01-01", Gender.MALE);
        user.chargePoint(Money.of(100000));
        entityManager.persist(user);

        Brand brand = Brand.createBrand("테스트브랜드");
        entityManager.persist(brand);

        Product product = Product.createProduct("P001", "테스트상품", Money.of(25000), 100, brand);
        entityManager.persist(product);

        Map<Product, Integer> productQuantities = new HashMap<>();
        productQuantities.put(product, 3);

        // when
        orderService.createOrder(user, productQuantities);

        entityManager.flush();
        entityManager.clear();

        // then
        Product productAfterOrder = entityManager.find(Product.class, product.getId());
        assertThat(productAfterOrder.getStock()).isEqualTo(Stock.of(97)); // 100 - 3
    }

    @DisplayName("주문 생성 시 사용자의 포인트를 차감한다.")
    @Test
    @Transactional
    void createOrder_usePoint_success() {
        // given
        User user = User.createUser("testUser", "test@test.com", "1990-01-01", Gender.MALE);
        user.chargePoint(Money.of(100000));
        entityManager.persist(user);

        Brand brand = Brand.createBrand("테스트브랜드");
        entityManager.persist(brand);

        Product product = Product.createProduct("P001", "테스트상품", Money.of(25000), 100, brand);
        entityManager.persist(product);

        Map<Product, Integer> productQuantities = new HashMap<>();
        productQuantities.put(product, 2); // 25000 * 2 = 50000

        // when
        orderService.createOrder(user, productQuantities);

        entityManager.flush();
        entityManager.clear();

        // then
        User userAfterOrder = entityManager.find(User.class, user.getId());
        assertThat(userAfterOrder.getPoint().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(50000)); // 100000 - 50000
    }

    @DisplayName("주문 생성 시 여러 상품의 재고와 포인트를 차감한다.")
    @Test
    @Transactional
    void createOrder_withMultipleProducts_success() {
        // given
        User user = User.createUser("testUser", "test@test.com", "1990-01-01", Gender.MALE);
        user.chargePoint(Money.of(200000));
        entityManager.persist(user);

        Brand brand = Brand.createBrand("테스트브랜드");
        entityManager.persist(brand);

        Product product1 = Product.createProduct("P001", "상품1", Money.of(25000), 100, brand);
        Product product2 = Product.createProduct("P002", "상품2", Money.of(10000), 50, brand);
        entityManager.persist(product1);
        entityManager.persist(product2);

        Map<Product, Integer> productQuantities = new HashMap<>();
        productQuantities.put(product1, 2); // 25000 * 2 = 50000
        productQuantities.put(product2, 3); // 10000 * 3 = 30000
        // 총 80000원

        // when
        orderService.createOrder(user, productQuantities);

        entityManager.flush();
        entityManager.clear();

        // then
        User userAfterOrder = entityManager.find(User.class, user.getId());
        Product product1AfterOrder = entityManager.find(Product.class, product1.getId());
        Product product2AfterOrder = entityManager.find(Product.class, product2.getId());

        assertAll(
                () -> assertThat(userAfterOrder.getPoint().getAmount()).isEqualByComparingTo(Money.of(120000).getAmount()), // 200000 - 80000
                () -> assertThat(product1AfterOrder.getStock()).isEqualTo(Stock.of(98)), // 100 - 2
                () -> assertThat(product2AfterOrder.getStock()).isEqualTo(Stock.of(47)) // 50 - 3
        );
    }

    @DisplayName("주문이 정상적으로 저장된다.")
    @Test
    @Transactional
    void createOrder_saveOrder_success() {
        // given
        User user = User.createUser("testUser", "test@test.com", "1990-01-01", Gender.MALE);
        user.chargePoint(Money.of(100000));
        entityManager.persist(user);

        Brand brand = Brand.createBrand("테스트브랜드");
        entityManager.persist(brand);

        Product product = Product.createProduct("P001", "테스트상품", Money.of(25000), 100, brand);
        entityManager.persist(product);

        Map<Product, Integer> productQuantities = new HashMap<>();
        productQuantities.put(product, 1);

        // when
        Order order = orderService.createOrder(user, productQuantities);

        // then
        assertAll(
                () -> assertThat(order.getId()).isNotNull(),
                () -> assertThat(order.getStatus()).isEqualTo(OrderStatus.INIT),
                () -> assertThat(order.getTotalPrice().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(25000)),
                () -> assertThat(order.getOrderProducts()).hasSize(1)
        );
    }
}
