package com.loopers.application;

import com.loopers.application.order.OrderCommand;
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
import com.loopers.utils.DatabaseCleanUp;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
@SpringBootTest
public class OrderConcurrencyTest {

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

    private static final int THREAD_COUNT = 10;
    private ExecutorService executor;


    @BeforeEach
    void setUp() {
        executor = Executors.newFixedThreadPool(THREAD_COUNT);
    }

    @AfterEach
    void tearDown() {
        executor.shutdown();
        databaseCleanUp.truncateAllTables();
    }

    @Test
    @DisplayName("포인트 동시성 테스트 - 동일한 유저가, 서로 다른 주문을 동시에 수행하는 경우")
    void concurrencyTest_sameUserOrdersThenPointsDeductedExactlyOnceEach() throws Exception {
        int initPoint = 100_000;
        int orderPrice = 5_000;
        int tryCount = 5;

        UserModel user = userRepository.save(new UserModel(
                "testuser1",
                "user2",
                "유저테스트",
                "a@b.com",
                "1997-09-28",
                "M",
                initPoint));

        BrandModel brand = brandRepository.save(new BrandModel(
                "테스트브랜드",
                "테스트브랜드입니다.",
                BrandStatus.REGISTERED));

        ProductModel product = productRepository.save(new ProductModel(
                "상품B",
                "테스트상품",
                orderPrice,
                100,
                ProductStatus.ON_SALE,
                brand));

        OrderCommand.OrderLine line = new OrderCommand.OrderLine(product.getId(), 1);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(tryCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        for (int i = 0; i < tryCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();

                    OrderCommand.Order command = new OrderCommand.Order(
                            user.getUserId(),
                            List.of(line)
                    );

                    userOrderProductFacade.placeOrder(command);
                    successCount.incrementAndGet();
                } catch (CoreException e) {
                    // 포인트 부족 등
                    failCount.incrementAndGet();
                    log.error("에러 발생", e.getMessage(), e);
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    log.error("그 외 에러 발생", e.getMessage(), e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // when
        startLatch.countDown();
        doneLatch.await();

        // then
        UserModel reloaded = userRepository.findById(user.getId()).orElseThrow();
        int expectedPoint = initPoint - (successCount.get() * orderPrice);

        assertThat(reloaded.getPoint()).isEqualTo(expectedPoint);
        assertThat(reloaded.getPoint()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("재고 동시성 테스트 - 동일한 상품에 대해 동시에 주문이 여러번 들어올 때")
    void concurrencyTest_orderSameroductThenStockDecraseCorrectly() throws Exception {
        int initStock = 30;
        int orderQty = 1;
        int initPoint = 100_000;
        int productPrice = 5_000;
        int tryCount = 10;

        UserModel user1 = userRepository.save(new UserModel(
                "testuser1",
                "user1",
                "유저테스트",
                "a@b.com",
                "1997-09-28",
                "M",
                initPoint));

        BrandModel brand = brandRepository.save(new BrandModel(
                "테스트브랜드",
                "테스트브랜드입니다.",
                BrandStatus.REGISTERED));

        ProductModel product = productRepository.save(new ProductModel(
                "상품B",
                "테스트상품",
                productPrice,
                initStock,
                ProductStatus.ON_SALE,
                brand));

        OrderCommand.OrderLine line = new OrderCommand.OrderLine(product.getId(), orderQty);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(tryCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        for (int i = 0; i < tryCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();

                    OrderCommand.Order command = new OrderCommand.Order(
                            user1.getUserId(),
                            List.of(line)
                    );

                    userOrderProductFacade.placeOrder(command);
                    successCount.incrementAndGet();
                } catch (CoreException e) {
                    // 재고 부족 등
                    log.error("에러 발생", e.getMessage(), e);
                    failCount.incrementAndGet();
                } catch (Exception e) {
                    log.error("그 외 에러 발생", e.getMessage(), e);
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // when
        startLatch.countDown();
        doneLatch.await();

        // then
        ProductModel reloaded = productRepository.findById(product.getId()).orElseThrow();
        int expectedStock = initStock - (successCount.get() * orderQty);

        assertThat(reloaded.getStock()).isEqualTo(expectedStock);   // 예: 0
        assertThat(reloaded.getStock()).isGreaterThanOrEqualTo(0); // 음수 안됨


    }



}
