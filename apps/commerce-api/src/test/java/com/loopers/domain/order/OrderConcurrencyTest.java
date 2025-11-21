package com.loopers.domain.order;

import com.loopers.application.members.MemberFacade;
import com.loopers.application.order.OrderCommand;
import com.loopers.application.order.OrderFacade;
import com.loopers.application.order.OrderLineCommand;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.repository.BrandRepository;
import com.loopers.domain.common.vo.Money;
import com.loopers.domain.members.enums.Gender;
import com.loopers.domain.points.Point;
import com.loopers.domain.points.repository.PointRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.repository.ProductRepository;
import com.loopers.domain.product.vo.Stock;
import com.loopers.support.error.CoreException;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("주문 동시성 테스트 (재고)")
class OrderConcurrencyTest {

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private MemberFacade memberFacade;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private PointRepository pointRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @Test
    @DisplayName("동일한 상품에 대해 여러 주문이 동시에 요청되어도, 재고가 정상적으로 차감되어야 한다")
    void shouldDeductStockCorrectly_whenConcurrentOrders() throws InterruptedException {
        // given
        Brand brand = brandRepository.save(new Brand("TestBrand", "Test Brand Description"));
        int initialStock = 10;
        Product product = new Product(
                brand.getId(),
                "Test Product",
                "Test Description",
                Money.of(BigDecimal.valueOf(1000)),
                Stock.of(initialStock)
        );
        Product savedProduct = productRepository.save(product);
        Long productId = savedProduct.getId();

        int threadCount = 10;

        // 10명의 회원 생성 및 포인트 부여
        for (int i = 0; i < threadCount; i++) {
            String memberId = "member" + i;
            memberFacade.registerMember(memberId, memberId + "@test.com", "password", "1990-01-01", Gender.MALE);
            // MemberFacade가 이미 Point를 0원으로 생성했으므로 업데이트
            Point existingPoint = pointRepository.findByMemberId(memberId).orElseThrow();
            existingPoint.addAmount(BigDecimal.valueOf(10000));
            pointRepository.save(existingPoint);
        }

        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when: 10명이 동시에 1개씩 주문
        try (ExecutorService executorService = Executors.newFixedThreadPool(threadCount)) {
            for (int i = 0; i < threadCount; i++) {
                final String memberId = "member" + i;
                executorService.submit(() -> {
                    try {
                        OrderCommand command = OrderCommand.of(
                                memberId,
                                List.of(OrderLineCommand.of(productId, 1))
                        );
                        orderFacade.placeOrder(command);
                        successCount.incrementAndGet();
                    } catch (CoreException e) {
                        failCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
        }

        // then: 재고만큼만 성공해야 함
        assertThat(successCount.get()).isEqualTo(initialStock);
        assertThat(failCount.get()).isEqualTo(threadCount - initialStock);

        Product result = productRepository.findById(productId).orElseThrow();
        assertThat(result.getStock().getQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("재고가 부족한 상황에서 동시 주문 시, 일부만 성공하고 재고는 음수가 되지 않아야 한다")
    void shouldRejectOrders_whenStockInsufficient() throws InterruptedException {
        // given
        Brand brand = brandRepository.save(new Brand("TestBrand", "Test Brand Description"));
        int initialStock = 5;
        Product product = new Product(
                brand.getId(),
                "Test Product",
                "Test Description",
                Money.of(BigDecimal.valueOf(1000)),
                Stock.of(initialStock)
        );
        Product savedProduct = productRepository.save(product);
        Long productId = savedProduct.getId();

        int threadCount = 10;

        // 10명의 회원 생성 및 포인트 부여
        for (int i = 0; i < threadCount; i++) {
            String memberId = "member" + i;
            memberFacade.registerMember(memberId, memberId + "@test.com", "password", "1990-01-01", Gender.MALE);
            // MemberFacade가 이미 Point를 0원으로 생성했으므로 업데이트
            Point existingPoint = pointRepository.findByMemberId(memberId).orElseThrow();
            existingPoint.addAmount(BigDecimal.valueOf(10000));
            pointRepository.save(existingPoint);
        }

        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when: 10명이 동시에 1개씩 주문 (재고는 5개)
        try (ExecutorService executorService = Executors.newFixedThreadPool(threadCount)) {
            for (int i = 0; i < threadCount; i++) {
                final String memberId = "member" + i;
                executorService.submit(() -> {
                    try {
                        OrderCommand command = OrderCommand.of(
                                memberId,
                                List.of(OrderLineCommand.of(productId, 1))
                        );
                        orderFacade.placeOrder(command);
                        successCount.incrementAndGet();
                    } catch (CoreException e) {
                        failCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
        }

        // then: 5건만 성공, 5건 실패
        assertThat(successCount.get()).isEqualTo(initialStock);
        assertThat(failCount.get()).isEqualTo(threadCount - initialStock);

        Product result = productRepository.findById(productId).orElseThrow();
        assertThat(result.getStock().getQuantity()).isEqualTo(0);
        assertThat(result.getStock().getQuantity()).isGreaterThanOrEqualTo(0); // 음수 방지 확인
    }

    @Test
    @DisplayName("여러 상품을 포함한 주문이 동시에 발생해도, 각 상품의 재고가 정확히 차감되어야 한다")
    void shouldDeductMultipleProductStocksCorrectly_whenConcurrentOrdersWithMultipleItems() throws InterruptedException {
        // given
        Brand brand = brandRepository.save(new Brand("TestBrand", "Test Brand Description"));

        Product product1 = productRepository.save(new Product(
                brand.getId(),
                "Product 1",
                "Description 1",
                Money.of(BigDecimal.valueOf(1000)),
                Stock.of(10)
        ));

        Product product2 = productRepository.save(new Product(
                brand.getId(),
                "Product 2",
                "Description 2",
                Money.of(BigDecimal.valueOf(2000)),
                Stock.of(10)
        ));

        int threadCount = 5;

        // 5명의 회원 생성 및 포인트 부여
        for (int i = 0; i < threadCount; i++) {
            String memberId = "member" + i;
            memberFacade.registerMember(memberId, memberId + "@test.com", "password", "1990-01-01", Gender.MALE);
            Point existingPoint = pointRepository.findByMemberId(memberId).orElseThrow();
            existingPoint.addAmount(BigDecimal.valueOf(50000));
            pointRepository.save(existingPoint);
        }

        CountDownLatch latch = new CountDownLatch(threadCount);

        // when: 5명이 동시에 각 상품 2개씩 주문
        try (ExecutorService executorService = Executors.newFixedThreadPool(threadCount)) {
            for (int i = 0; i < threadCount; i++) {
                final String memberId = "member" + i;
                executorService.submit(() -> {
                    try {
                        OrderCommand command = OrderCommand.of(
                                memberId,
                                List.of(
                                        OrderLineCommand.of(product1.getId(), 2),
                                        OrderLineCommand.of(product2.getId(), 2)
                                )
                        );
                        orderFacade.placeOrder(command);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
        }

        // then
        Product result1 = productRepository.findById(product1.getId()).orElseThrow();
        Product result2 = productRepository.findById(product2.getId()).orElseThrow();

        assertThat(result1.getStock().getQuantity()).isEqualTo(0); // 10 - (5 * 2) = 0
        assertThat(result2.getStock().getQuantity()).isEqualTo(0); // 10 - (5 * 2) = 0
    }
}
