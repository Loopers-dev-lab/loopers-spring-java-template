package com.loopers.application.order;

import com.loopers.domain.Money;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.Stock;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserRepository;
import com.loopers.interfaces.api.order.OrderV1Dto;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@ActiveProfiles("test")
@SpringBootTest
class OrderFacadeIntegrationTest {
    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private BrandRepository brandRepository;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("동시에 주문이 처리되어도 정상적으로 재고가 차감된다.")
    @Test
    void concurrentOrder_decreaseStock_success() throws Exception {
        // given - 초기 데이터 준비 (Repository 사용으로 자동 commit)
        Brand brand = Brand.createBrand("테스트브랜드");
        Brand savedBrand = brandRepository.registerBrand(brand);

        // 초기 재고 100개인 상품 생성 (영속 상태의 Brand 사용)
        Product product = Product.createProduct("P001", "테스트상품", Money.of(1000), 100, savedBrand);
        Product savedProduct = productRepository.registerProduct(product);

        // 10명의 사용자 생성
        List<String> userIds = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String userId = "user" + i;
            User user = User.createUser(userId, "test" + i + "@test.com", "1990-01-01", Gender.MALE);
            user.chargePoint(Money.of(100000)); // 충분한 포인트
            userRepository.save(user);
            userIds.add(userId);
        }

        Long productId = savedProduct.getId();

        // when - 10개의 쓰레드에서 동시에 주문
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    List<OrderV1Dto.OrderRequest.OrderItemRequest> items = List.of(
                        new OrderV1Dto.OrderRequest.OrderItemRequest(productId, 1)
                    );
                    orderFacade.createOrder(userIds.get(index), items);
                } catch (Exception e) {
                    // 예외 발생 시 로그만 출력 (데드락 무시)
                    System.out.println("주문 실패 (예상된 동시성 이슈): " + e.getMessage());
                }
            }, executor);
            futures.add(future);
        }

        // 모든 쓰레드 완료 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();

        // then - 재고 불일치 확인 (Lost Update 발생 가능)
        Thread.sleep(1000); // DB 반영 대기
        entityManager.clear();
        Product productAfterOrder = entityManager.find(Product.class, productId);

        System.out.println("===== 동시성 테스트 결과 =====");
        System.out.println("예상 재고: 90 (100 - 10)");
        System.out.println("실제 재고: " + productAfterOrder.getStockQuantity());
        System.out.println("차이: " + (90 - productAfterOrder.getStockQuantity()) + "개 누락 (Lost Update)");

        // 재고 불일치 확인 (실패할 것으로 예상)
        assertThat(productAfterOrder.getStock()).isEqualTo(Stock.of(90)); // 100 - 10
    }

    @DisplayName("주문 생성 시 상품의 재고를 차감한다.")
    @Test
    @Transactional
    void createOrder_decreaseStock_success() {
        // given
        String userId = "testUser";
        User user = User.createUser(userId, "test@test.com", "1990-01-01", Gender.MALE);
        user.chargePoint(Money.of(100000));
        entityManager.persist(user);

        Brand brand = Brand.createBrand("테스트브랜드");
        entityManager.persist(brand);

        Product product = Product.createProduct("P001", "테스트상품", Money.of(25000), 100, brand);
        entityManager.persist(product);

        List<OrderV1Dto.OrderRequest.OrderItemRequest> items = List.of(
                new OrderV1Dto.OrderRequest.OrderItemRequest(product.getId(), 3)
        );

        // when
        orderFacade.createOrder(userId, items);

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
        String userId = "testUser";
        User user = User.createUser(userId, "test@test.com", "1990-01-01", Gender.MALE);
        user.chargePoint(Money.of(100000));
        entityManager.persist(user);

        Brand brand = Brand.createBrand("테스트브랜드");
        entityManager.persist(brand);

        Product product = Product.createProduct("P001", "테스트상품", Money.of(25000), 100, brand);
        entityManager.persist(product);

        List<OrderV1Dto.OrderRequest.OrderItemRequest> items = List.of(
                new OrderV1Dto.OrderRequest.OrderItemRequest(product.getId(), 2)
        );

        // when
        orderFacade.createOrder(userId, items);

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
        String userId = "testUser";
        User user = User.createUser(userId, "test@test.com", "1990-01-01", Gender.MALE);
        user.chargePoint(Money.of(200000));
        entityManager.persist(user);

        Brand brand = Brand.createBrand("테스트브랜드");
        entityManager.persist(brand);

        Product product1 = Product.createProduct("P001", "상품1", Money.of(25000), 100, brand);
        Product product2 = Product.createProduct("P002", "상품2", Money.of(10000), 50, brand);
        entityManager.persist(product1);
        entityManager.persist(product2);

        List<OrderV1Dto.OrderRequest.OrderItemRequest> items = List.of(
                new OrderV1Dto.OrderRequest.OrderItemRequest(product1.getId(), 2),
                new OrderV1Dto.OrderRequest.OrderItemRequest(product2.getId(), 3)
        );

        // when
        orderFacade.createOrder(userId, items);

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
        String userId = "testUser";
        User user = User.createUser(userId, "test@test.com", "1990-01-01", Gender.MALE);
        user.chargePoint(Money.of(100000));
        entityManager.persist(user);

        Brand brand = Brand.createBrand("테스트브랜드");
        entityManager.persist(brand);

        Product product = Product.createProduct("P001", "테스트상품", Money.of(25000), 100, brand);
        entityManager.persist(product);

        Map<Product, Integer> productQuantities = new HashMap<>();
        productQuantities.put(product, 1);

        List<OrderV1Dto.OrderRequest.OrderItemRequest> items = List.of(
                new OrderV1Dto.OrderRequest.OrderItemRequest(product.getId(), 1)
        );

        // when
        Order order = orderFacade.createOrder(userId, items);

        // then
        assertAll(
                () -> assertThat(order.getId()).isNotNull(),
                () -> assertThat(order.getStatus()).isEqualTo(OrderStatus.INIT),
                () -> assertThat(order.getTotalPrice().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(25000)),
                () -> assertThat(order.getOrderItems()).hasSize(1)
        );
    }
}
