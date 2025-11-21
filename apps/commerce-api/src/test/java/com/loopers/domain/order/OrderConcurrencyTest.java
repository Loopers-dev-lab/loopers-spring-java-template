package com.loopers.domain.order;

import com.loopers.application.order.OrderFacade;
import com.loopers.application.order.OrderItemInfo;
import com.loopers.application.point.PointFacade;
import com.loopers.domain.common.vo.Money;
import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.stock.Stock;
import com.loopers.domain.stock.StockRepository;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static com.loopers.support.fixture.UserFixtures.createValidUser;
import static com.loopers.support.fixture.UserFixtures.createValidUsers;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest
public class OrderConcurrencyTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private PointRepository pointRepository;

    @Autowired
    private PointFacade pointFacade;

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private final int threadCount = 3;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    /**
     * 특정 유저에게 포인트를 생성하고 충전
     */
    private void createAndChargePoint(Long userId, int amount) {
        Point point = pointRepository.save(Point.create(userId));
        point.charge(Money.of(amount));
        pointRepository.save(point);
    }

    /**
     * 상품과 재고를 생성
     */
    private Product createProductAndSaveStock(Long i, int price, int stockQuantity) {
        Product product = productRepository.save(Product.create(i, "상품이름"+i, Money.of(price)));
        stockRepository.save(Stock.create(product.getId(), stockQuantity));
        return product;
    }

    @DisplayName("한 사용자가 주문 3건을 동시에 등록해도 포인트는 정상차감되어야한다.")
    @Test
    public void concurrentOrders_shouldDeductPointCorrectly_whenUserCreatesMultipleOrders() throws InterruptedException {
        // arrange
        // 스레드 풀 생성
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        User user = userRepository.save(createValidUser());
        createAndChargePoint(user.getId(), 10000);

        Product product = createProductAndSaveStock(1L, 1000, 3);
        OrderItemInfo itemInfo = OrderItemInfo.of(product.getId(), 1);

        // act
        List<CompletableFuture<Void>> futures = IntStream.range(0, threadCount)
                .mapToObj(i ->
                        CompletableFuture.runAsync(() -> {
                            orderFacade.createOrder(user.getId(), List.of(itemInfo));
                            }, executorService)
                )
                .toList();

        // 모든 작업이 끝날때 까지 기다림
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        executorService.shutdown();

        // assert
        Point updatedPoint = pointRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(updatedPoint.getBalance()).isEqualTo(Money.of(7000));
    }

    @DisplayName("한 사용자가 주문 3건을 동시에 등록하지만, 포인트가 부족해지면 주문은 실패한다.")
    @Test
    public void concurrentOrders_shouldFailWhenPointInsufficient() {
        // arrange
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        User user = userRepository.save(createValidUser());
        createAndChargePoint(user.getId(), 2000);

        Product product = createProductAndSaveStock(1L, 1000, 3);
        OrderItemInfo itemInfo = OrderItemInfo.of(product.getId(), 1);

        // act
        List<CompletableFuture<String>> futures = IntStream.range(0, threadCount)
                .mapToObj(i ->
                        CompletableFuture.supplyAsync(() -> {
                            try {
                                orderFacade.createOrder(user.getId(), List.of(itemInfo));
                                return "SUCCESS";
                            } catch (Exception e) {
                                return "FAIL";
                            }
                        }, executorService)
                )
                .toList();


        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        executorService.shutdown();

        // assert
        List<String> results = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        assertAll(
                () -> assertThat(results.stream().filter(s -> s.equals("SUCCESS")).count()).isEqualTo(2),
                () -> assertThat(results.stream().filter(s -> s.equals("FAIL")).count()).isEqualTo(1)
        );


    }

    @DisplayName("재고가 3개인 상품을 3명의 유저가 동시에 1개씩 주문하면, 재고는 0이 되어야한다.")
    @Test
    public void concurrentOrders_shouldReduceStockToZero_whenStockExactlyMatchesOrderCount() throws InterruptedException {
        // arrange
        int threadCount = 3;

        // 3개의 스레드를 가질 수 있는 스레드 풀 생성
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        List<User> users = createValidUsers(threadCount);
        IntStream.range(0, threadCount).forEach(i -> {
                User user = userRepository.save(users.get(i));
                createAndChargePoint(user.getId(), 10000);
            }
        );

        Product product = createProductAndSaveStock(1L, 1000, 3);
        OrderItemInfo itemInfo = OrderItemInfo.of(product.getId(), 1);

        // act
        List<CompletableFuture<Void>> futures = IntStream.range(0, threadCount)
                .mapToObj(i ->
                        CompletableFuture.runAsync(() -> {
                            orderFacade.createOrder(users.get(i).getId(), List.of(itemInfo));
                        }, executorService)
                )
                .toList();

        // 모든 작업이 끝날때 까지 기다림
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        executorService.shutdown();

        // assert
        Stock stock = stockRepository.findByProductId(product.getId()).orElseThrow();
        assertThat(stock.getQuantity()).isEqualTo(0);

    }

    @DisplayName("재고가 1개인 상품을 3명의 유저가 동시에 1개씩 주문하면, 두명은 주문 실패한다. (Lost Update 방지)")
    @Test
    public void concurrentOrders_shouldAllowOnlyOneSuccess_whenStockIsOne() {
        // arrange
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        List<User> users = createValidUsers(threadCount);
        IntStream.range(0, threadCount).forEach(i -> {
                    User user = userRepository.save(users.get(i));
                    createAndChargePoint(user.getId(), 10000);
                }
        );

        Product product = createProductAndSaveStock(1L, 1000, 1);
        OrderItemInfo itemInfo = OrderItemInfo.of(product.getId(), 1);

        List<CompletableFuture<String>> futures = IntStream.range(0, threadCount)
                .mapToObj(i ->
                        CompletableFuture.supplyAsync(() -> {
                            try {
                                orderFacade.createOrder(users.get(i).getId(), List.of(itemInfo));
                                return "SUCCESS";
                            } catch (Exception e) {
                                return "FAIL";
                            }
                        }, executorService)
                )
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        executorService.shutdown();

        // assert
        List<String> results = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        Stock updatedStock = stockRepository.findByProductId(product.getId()).orElseThrow();
        assertAll(
                () -> assertThat(updatedStock.getQuantity()).isEqualTo(0),
                () -> assertThat(results.stream().filter(s -> s.equals("SUCCESS")).count()).isEqualTo(1),
                () -> assertThat(results.stream().filter(s -> s.equals("FAIL")).count()).isEqualTo(2)
        );

    }

    @DisplayName("2명의 유저가 동일한 상품목록을 다른 순서로 주문 시, 재고/포인트가 충분하면 주문 성공한다 (DeadLock 방지)")
    @Test
    public void concurrentOrders_shouldSucceedWithoutDeadlock_whenOrderItemSequenceDiffers() {
        // arrange
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        List<User> users = createValidUsers(2);
        IntStream.range(0, 2).forEach(i -> {
                    User user = userRepository.save(users.get(i));
                    createAndChargePoint(user.getId(), 10000);
                }
        );

        Product product1 = createProductAndSaveStock(1L, 1000, 2);
        Product product2 = createProductAndSaveStock(2L, 2000, 2);

        List<OrderItemInfo> orderItemInfos = new ArrayList<>();
        orderItemInfos.add(OrderItemInfo.of(product1.getId(), 1));
        orderItemInfos.add(OrderItemInfo.of(product2.getId(), 1));
        List<OrderItemInfo> orderItemInfosReversed = orderItemInfos.stream()
                .sorted(Comparator.comparing(OrderItemInfo::productId).reversed())
                .toList();

        List<CompletableFuture<Void>> futures = IntStream.range(0, 2)
                .mapToObj(i ->
                        CompletableFuture.runAsync(() -> {
                            if(i == 0) {
                                orderFacade.createOrder(users.get(i).getId(), orderItemInfos);
                            }else {
                                orderFacade.createOrder(users.get(i).getId(), orderItemInfosReversed);
                            }
                        }, executorService)
                ).toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        executorService.shutdown();

        Stock updatedStock1 = stockRepository.findByProductId(product1.getId()).orElseThrow();
        Stock updatedStock2 = stockRepository.findByProductId(product2.getId()).orElseThrow();
        Point updatedPoint1 = pointRepository.findByUserId(users.get(0).getId()).orElseThrow();
        Point updatedPoint2 = pointRepository.findByUserId(users.get(1).getId()).orElseThrow();
        assertAll(
                () -> assertThat(updatedStock1.getQuantity()).isEqualTo(0),
                () -> assertThat(updatedStock2.getQuantity()).isEqualTo(0),
                () -> assertThat(updatedPoint1.getBalance().getAmount().intValue()).isEqualTo(7000),
                () -> assertThat(updatedPoint2.getBalance().getAmount().intValue()).isEqualTo(7000)
        );

    }

}
