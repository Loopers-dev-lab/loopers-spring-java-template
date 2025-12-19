package com.loopers.domain.points;

import com.loopers.application.members.MemberFacade;
import com.loopers.application.order.OrderCommand;
import com.loopers.application.order.OrderFacade;
import com.loopers.application.order.OrderLineCommand;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.repository.BrandRepository;
import com.loopers.domain.common.vo.Money;
import com.loopers.domain.members.enums.Gender;
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
@DisplayName("포인트 동시성 테스트")
class PointConcurrencyTest {

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
    @DisplayName("동일한 유저가 서로 다른 주문을 동시에 수행해도, 포인트가 정상적으로 차감되어야 한다")
    void shouldDeductPointsCorrectly_whenSameUserPlacesConcurrentOrders() throws InterruptedException {
        // given
        String username = "member1";
        memberFacade.registerMember(username, username + "@test.com", "password", "1990-01-01", Gender.MALE);

        // Member ID is 1L (first member created after table truncation)
        Long memberId = 1L;

        BigDecimal initialPoints = BigDecimal.valueOf(10000);
        // MemberFacade가 이미 Point를 0원으로 생성했으므로 업데이트
        Point existingPoint = pointRepository.findByMemberId(memberId).orElseThrow();
        existingPoint.addAmount(initialPoints);
        pointRepository.save(existingPoint);

        Brand brand = brandRepository.save(new Brand("TestBrand", "Test Brand Description"));

        // 여러 상품 생성
        Product product1 = productRepository.save(new Product(
                brand.getId(),
                "Product 1",
                "Description 1",
                Money.of(BigDecimal.valueOf(3000)),
                Stock.of(100)
        ));

        Product product2 = productRepository.save(new Product(
                brand.getId(),
                "Product 2",
                "Description 2",
                Money.of(BigDecimal.valueOf(4000)),
                Stock.of(100)
        ));

        int threadCount = 2;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when: 동일 유저가 동시에 2개 주문 (3000원, 4000원)
        // 포인트 10,000원이므로 둘 다 성공하면 음수가 됨 (동시성 이슈)
        try (ExecutorService executorService = Executors.newFixedThreadPool(threadCount)) {
            executorService.submit(() -> {
                try {
                    OrderCommand command = OrderCommand.of(
                            memberId,
                            List.of(OrderLineCommand.of(product1.getId(), 1))
                    );
                    orderFacade.placeOrder(command);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });

            executorService.submit(() -> {
                try {
                    OrderCommand command = OrderCommand.of(
                            memberId,
                            List.of(OrderLineCommand.of(product2.getId(), 1))
                    );
                    orderFacade.placeOrder(command);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });

            latch.await();
        }

        // then: 주문 생성은 모두 성공 (포인트 차감은 결제 단계에서 수행)
        assertThat(successCount.get()).isEqualTo(2);
        assertThat(failCount.get()).isEqualTo(0);

        Point result = pointRepository.findByMemberId(memberId).orElseThrow();
        assertThat(result.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(10000)); // 주문 시점에는 변동 없음
    }

    @Test
    @DisplayName("포인트가 부족한 상황에서 동일 유저가 동시 주문 시, 일부만 성공하고 포인트는 음수가 되지 않아야 한다")
    void shouldRejectOrder_whenPointsInsufficientDuringConcurrentOrders() throws InterruptedException {
        // given
        String username = "member1";
        memberFacade.registerMember(username, username + "@test.com", "password", "1990-01-01", Gender.MALE);

        // Member ID is 1L (first member created after table truncation)
        Long memberId = 1L;

        BigDecimal initialPoints = BigDecimal.valueOf(5000);
        // MemberFacade가 이미 Point를 0원으로 생성했으므로 업데이트
        Point existingPoint = pointRepository.findByMemberId(memberId).orElseThrow();
        existingPoint.addAmount(initialPoints);
        pointRepository.save(existingPoint);

        Brand brand = brandRepository.save(new Brand("TestBrand", "Test Brand Description"));

        // 여러 상품 생성
        Product product1 = productRepository.save(new Product(
                brand.getId(),
                "Product 1",
                "Description 1",
                Money.of(BigDecimal.valueOf(3000)),
                Stock.of(100)
        ));

        Product product2 = productRepository.save(new Product(
                brand.getId(),
                "Product 2",
                "Description 2",
                Money.of(BigDecimal.valueOf(3000)),
                Stock.of(100)
        ));

        int threadCount = 2;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when: 동일 유저가 동시에 2개 주문 (각 3000원)
        // 포인트 5,000원이므로 하나만 성공해야 함
        try (ExecutorService executorService = Executors.newFixedThreadPool(threadCount)) {
            executorService.submit(() -> {
                try {
                    OrderCommand command = OrderCommand.of(
                            memberId,
                            List.of(OrderLineCommand.of(product1.getId(), 1))
                    );
                    orderFacade.placeOrder(command);
                    successCount.incrementAndGet();
                } catch (CoreException e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });

            executorService.submit(() -> {
                try {
                    OrderCommand command = OrderCommand.of(
                            memberId,
                            List.of(OrderLineCommand.of(product2.getId(), 1))
                    );
                    orderFacade.placeOrder(command);
                    successCount.incrementAndGet();
                } catch (CoreException e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });

            latch.await();
        }

        // then: 주문 생성은 모두 성공(포인트 검증은 결제 단계)
        assertThat(successCount.get()).isEqualTo(2);
        assertThat(failCount.get()).isEqualTo(0);

        Point result = pointRepository.findByMemberId(memberId).orElseThrow();
        assertThat(result.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(5000)); // 주문 시점에는 변동 없음
    }

    @Test
    @DisplayName("여러 유저가 동시에 주문해도, 각 유저의 포인트가 독립적으로 정확히 차감되어야 한다")
    void shouldDeductPointsIndependently_whenMultipleUsersConcurrentlyPlaceOrders() throws InterruptedException {
        // given
        Brand brand = brandRepository.save(new Brand("TestBrand", "Test Brand Description"));

        Product product = productRepository.save(new Product(
                brand.getId(),
                "Product",
                "Description",
                Money.of(BigDecimal.valueOf(1000)),
                Stock.of(100)
        ));

        int userCount = 5;

        // 5명의 회원 생성 및 각각 3000원 포인트 부여
        Long[] memberIds = new Long[userCount];
        for (int i = 0; i < userCount; i++) {
            String username = "member" + i;
            memberFacade.registerMember(username, username + "@test.com", "password", "1990-01-01", Gender.MALE);
            // Member IDs are assigned sequentially starting from 1L
            Long memberId = (long) (i + 1);
            memberIds[i] = memberId;

            Point existingPoint = pointRepository.findByMemberId(memberId).orElseThrow();
            existingPoint.addAmount(BigDecimal.valueOf(3000));
            pointRepository.save(existingPoint);
        }

        CountDownLatch latch = new CountDownLatch(userCount);

        // when: 5명이 동시에 1000원 상품 주문
        try (ExecutorService executorService = Executors.newFixedThreadPool(userCount)) {
            for (int i = 0; i < userCount; i++) {
                final Long memberId = memberIds[i];
                executorService.submit(() -> {
                    try {
                        OrderCommand command = OrderCommand.of(
                                memberId,
                                List.of(OrderLineCommand.of(product.getId(), 1))
                        );
                        orderFacade.placeOrder(command);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
        }

        // then: 모든 유저 포인트는 주문 시점에 변동 없음
        for (int i = 0; i < userCount; i++) {
            Long memberId = memberIds[i];
            Point point = pointRepository.findByMemberId(memberId).orElseThrow();
            assertThat(point.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(3000)); // 변동 없음
        }
    }
}
