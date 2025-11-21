package com.loopers.domain.order;

import com.loopers.application.order.CreateOrderCommand;
import com.loopers.application.order.OrderFacade;
import com.loopers.application.order.OrderItemCommand;
import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OrderConcurrencyTest {

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PointRepository pointRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("동일 유저가 동시에 여러 주문을 수행해도 포인트는 정확히 차감된다.")
    @Test
    void should_handle_point_concurrency_correctly() throws Exception {

        // 유저 생성
        String userId = "sameUser";
        Point point = Point.create(userId, 100_000L); // 충분한 포인트
        pointRepository.save(point);

        int threadCount = 10;

        List<Product> products = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            products.add(productRepository.save(
                    Product.create((long) (i + 1), "상품" + i, 10000L, 100L)
            ));
        }

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Boolean> results = new CopyOnWriteArrayList<>();

        IntStream.range(0, threadCount).forEach(i ->
                executor.submit(() -> {
                    try {
                        Product product = products.get(i);

                        CreateOrderCommand command = new CreateOrderCommand(
                                userId,
                                List.of(new OrderItemCommand(product.getId(), 1L))
                        );

                        orderFacade.createOrder(command);
                        results.add(true);
                    } catch (Exception e) {
                        results.add(false);
                    } finally {
                        latch.countDown();
                    }
                })
        );

        latch.await();
        executor.shutdown();

        int successCount = (int) results.stream().filter(v -> v).count();

        Point finalPoint = pointRepository.findByUserId(userId).orElseThrow();

        long expected = 100000L - (10000L * threadCount);

        assertThat(successCount).isEqualTo(threadCount);
        assertThat(finalPoint.getBalance()).isEqualTo(expected);
    }

    @DisplayName("동일 상품에 대해 여러 주문이 동시에 요청되어도 재고는 정확히 차감된다.")
    @Test
    void should_handle_stock_concurrency_correctly() throws Exception {

        Product product = Product.create(1L, "동시성테스트상품", 10000L, 10L);
        productRepository.save(product);

        String userId = "sameUser2";
        Point point = Point.create(userId, 100000L);
        pointRepository.save(point);

        int threadCount = 20;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Boolean> results = new CopyOnWriteArrayList<>();

        // when
        IntStream.range(0, threadCount).forEach(i ->
                executor.submit(() -> {
                    try {
                        CreateOrderCommand command = new CreateOrderCommand(
                                userId,
                                List.of(new OrderItemCommand(product.getId(), 1L))
                        );

                        orderFacade.createOrder(command);
                        results.add(true);
                    } catch (Exception e) {
                        results.add(false);
                    } finally {
                        latch.countDown();
                    }
                })
        );

        latch.await();
        executor.shutdown();

        // then
        int successCount = (int) results.stream().filter(v -> v).count();

        Product finalProduct = productRepository.findById(product.getId()).orElseThrow();
        Point finalPoint = pointRepository.findByUserId(userId).orElseThrow();

        long expectedPoint = 100000L - (10000L * successCount);

        assertThat(successCount).isEqualTo(10);
        assertThat(finalProduct.getStock()).isEqualTo(0);
        assertThat(finalPoint.getBalance()).isEqualTo(expectedPoint);
    }

}
