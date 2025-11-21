package com.loopers.domain.order;

import com.loopers.domain.point.Point;
import com.loopers.domain.product.Money;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.Stock;
import com.loopers.infrastructure.point.PointJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OrderConcurrencyIntegrationTest {

    @Autowired
    private OrderDomainService orderDomainService;
    @Autowired
    private PointJpaRepository pointJpaRepository;
    @Autowired
    private ProductJpaRepository productJpaRepository;
    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    private Product newProduct(String name, long price, int stock) {
        return Product.builder()
                .brandId(1L)
                .name(name)
                .price(new Money(BigDecimal.valueOf(price)))
                .stockQuantity(new Stock(stock))
                .build();
    }

    private OrderItem item(long productId, int quantity, long unitPrice) {
        return OrderItem.builder()
                .order(null)
                .productId(productId)
                .quantity(quantity)
                .price(BigDecimal.valueOf(unitPrice))
                .build();
    }

    @Test
    @DisplayName("동일한 유저가 서로 다른 주문을 동시에 수행해도, 포인트가 정상적으로 차감되어야 한다.")
    void concurrent_orders_deduct_points_correctly_with_pessimistic_lock() throws InterruptedException {
        // given
        String userId = "user-1";
        BigDecimal initial = BigDecimal.valueOf(100_000);
        Point point = Point.builder()
                .userId(userId)
                .pointAmount(initial)
                .build();
        pointJpaRepository.save(point);

        Product product = productJpaRepository.save(newProduct("P1", 1_000, 10_000));
        Long productId = product.getId();

        int orders = 50;
        int quantity = 1;
        long unitPrice = 1_000;

        ExecutorService pool = Executors.newFixedThreadPool(16);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(orders);
        List<Runnable> tasks = new ArrayList<>();

        for (int i = 0; i < orders; i++) {
            tasks.add(() -> {
                await(start);
                OrderItem orderItem = item(productId, quantity, unitPrice);
                orderDomainService.createOrder(userId, List.of(orderItem));
                done.countDown();
            });
        }
        tasks.forEach(pool::execute);

        // when
        start.countDown();
        done.await();
        pool.shutdown();

        // then
        Point reloaded = pointJpaRepository.findByUserId(userId).orElseThrow();
        BigDecimal expected = initial.subtract(BigDecimal.valueOf(orders * unitPrice));
        assertThat(reloaded.getPointAmount()).isEqualByComparingTo(expected);
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}


