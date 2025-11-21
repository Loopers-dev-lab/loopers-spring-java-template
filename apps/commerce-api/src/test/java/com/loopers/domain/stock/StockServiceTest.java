package com.loopers.domain.stock;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandStatus;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.ProductStatus;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Stock Service 테스트")
@SpringBootTest
class StockServiceTest {

    @Autowired
    private StockService stockService;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandJpaRepository brandJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private Long productId;
    private final Long initialStockQuantity = 100L;

    @BeforeEach
    void setUp() {
        // 테스트용 Brand 생성
        Brand brand = Brand.builder()
                .name("Test Brand")
                .description("Test Description")
                .status(BrandStatus.ON_SALE)
                .isVisible(true)
                .isSellable(true)
                .build();
        Brand savedBrand = brandJpaRepository.save(brand);

        // 테스트용 Product 생성
        Product product = Product.builder()
                .name("Test Product")
                .description("Test Description")
                .price(BigDecimal.valueOf(10000))
                .status(ProductStatus.ON_SALE)
                .isVisible(true)
                .isSellable(true)
                .brand(savedBrand)
                .build();
        Product savedProduct = productRepository.save(product)
                .orElseThrow(() -> new RuntimeException("Product 저장 실패"));
        productId = savedProduct.getId();

        // Stock quantity 세팅
        stockService.increaseQuantity(productId, initialStockQuantity);
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("decreaseQuantity 동시성 테스트")
    @Nested
    class DecreaseQuantityConcurrencyTest {

        @DisplayName("성공 케이스: 여러 스레드가 동시에 같은 상품의 재고를 차감해도 정확히 차감됨")
        @Test
        void decreaseQuantity_concurrentRequests_success() throws InterruptedException {
            // arrange
            int threadCount = 10;
            long decreaseQuantityPerThread = 5L;
            Long expectedFinalStock = initialStockQuantity - (threadCount * decreaseQuantityPerThread);

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);
            List<Exception> exceptions = new ArrayList<>();

            // act
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        stockService.decreaseQuantity(productId, decreaseQuantityPerThread);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                        exceptions.add(e);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // wait for all threads to complete
            assertTrue(latch.await(10, TimeUnit.SECONDS), "모든 스레드가 10초 내에 완료되지 않았습니다.");
            executor.shutdown();

            // assert
            Long finalStock = stockRepository.findByProductId(productId)
                    .map(Stock::getQuantity)
                    .orElseThrow(() -> new RuntimeException("재고를 찾을 수 없습니다"));

            assertEquals(expectedFinalStock, finalStock,
                    String.format("최종 재고는 예상값과 일치해야 함: 예상=%d, 실제=%d", expectedFinalStock, finalStock));
            assertEquals(threadCount, successCount.get(),
                    String.format("성공한 스레드 수는 전체 스레드 수와 일치해야 함: 예상=%d, 실제=%d", threadCount, successCount.get()));
            assertEquals(0, failureCount.get(),
                    String.format("실패한 스레드 수는 0이어야 함: 실제=%d", failureCount.get()));
            assertTrue(exceptions.isEmpty(),
                    String.format("예외가 발생하지 않아야 함: 예외 개수=%d", exceptions.size()));
        }

        @DisplayName("성공 케이스: 재고가 부족한 경우 일부만 성공하고 나머지는 실패함")
        @Test
        void decreaseQuantity_insufficientStock_partialSuccess() throws InterruptedException {
            // arrange
            int threadCount = 25; // 재고 100개에서 각 5개씩 차감하면 최대 20개 가능
            Long decreaseQuantityPerThread = 5L;

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);
            List<Exception> exceptions = new ArrayList<>();

            // act
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        stockService.decreaseQuantity(productId, decreaseQuantityPerThread);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                        exceptions.add(e);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // wait for all threads to complete
            assertTrue(latch.await(10, TimeUnit.SECONDS), "모든 스레드가 10초 내에 완료되지 않았습니다.");
            executor.shutdown();

            // assert
            Long finalStock = stockRepository.findByProductId(productId)
                    .map(Stock::getQuantity)
                    .orElseThrow(() -> new RuntimeException("재고를 찾을 수 없습니다"));

            // 최대 20개 차감 가능 (100 / 5 = 20)
            Long initialStock = 100L;
            int maxPossibleDecreases = (int) (initialStock / decreaseQuantityPerThread);
            assertTrue(successCount.get() <= maxPossibleDecreases,
                    String.format("성공한 차감 수는 최대 가능 차감 수 이하여야 함: 최대=%d, 실제=%d", maxPossibleDecreases, successCount.get()));
            assertEquals(threadCount, successCount.get() + failureCount.get(),
                    String.format("성공 수 + 실패 수는 전체 스레드 수와 일치해야 함: 예상=%d, 실제=%d", threadCount, successCount.get() + failureCount.get()));
            assertTrue(finalStock >= 0L,
                    String.format("최종 재고는 0 이상이어야 함: 실제=%d", finalStock));
            assertTrue(finalStock < decreaseQuantityPerThread,
                    String.format("남은 재고는 차감 단위보다 작아야 함: 차감 단위=%d, 남은 재고=%d", decreaseQuantityPerThread, finalStock));

            // 실패한 경우는 재고 부족 예외여야 함
            long stockInsufficientExceptions = exceptions.stream()
                    .filter(e -> e instanceof CoreException)
                    .filter(e -> ((CoreException) e).getErrorType() == ErrorType.BAD_REQUEST)
                    .filter(e -> e.getMessage().contains("재고가 부족"))
                    .count();
            assertEquals(failureCount.get(), stockInsufficientExceptions,
                    String.format("재고 부족 예외 수는 실패 수와 일치해야 함: 예상=%d, 실제=%d", failureCount.get(), stockInsufficientExceptions));
        }

        @DisplayName("성공 케이스: 재고 차감이 원자적으로 처리되어 Lost Update가 발생하지 않음")
        @Test
        void decreaseQuantity_atomicOperation_noLostUpdate() throws InterruptedException {
            // arrange
            int threadCount = 20;
            long decreaseQuantityPerThread = 1L;
            long initialStock = 100L;

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);

            // act
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        stockService.decreaseQuantity(productId, decreaseQuantityPerThread);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        // 실패는 무시 (재고 부족 등)
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // wait for all threads to complete
            assertTrue(latch.await(10, TimeUnit.SECONDS), "모든 스레드가 10초 내에 완료되지 않았습니다.");
            executor.shutdown();

            // assert
            Long finalStock = stockRepository.findByProductId(productId)
                    .map(Stock::getQuantity)
                    .orElseThrow(() -> new RuntimeException("재고를 찾을 수 없습니다"));

            // Lost Update가 발생하지 않았는지 확인
            // 성공한 차감 수만큼 정확히 차감되어야 함
            Long expectedStock = initialStock - (successCount.get() * decreaseQuantityPerThread);
            assertEquals(expectedStock, finalStock,
                    String.format("Lost Update가 발생하지 않아야 함: 예상 재고=%d, 실제 재고=%d, 성공한 차감 수=%d", expectedStock, finalStock, successCount.get()));
        }

        @DisplayName("성공 케이스: 동시에 여러 스레드가 재고를 차감해도 음수가 되지 않음")
        @Test
        void decreaseQuantity_concurrentRequests_noNegativeStock() throws InterruptedException {
            // arrange
            int threadCount = 25; // 재고 100개에서 각 5개씩 차감하면 최대 20개 가능
            long decreaseQuantityPerThread = 5L;

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);

            // act
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        stockService.decreaseQuantity(productId, decreaseQuantityPerThread);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // wait for all threads to complete
            assertTrue(latch.await(10, TimeUnit.SECONDS), "모든 스레드가 10초 내에 완료되지 않았습니다.");
            executor.shutdown();

            // assert
            Long finalStock = stockRepository.findByProductId(productId)
                    .map(Stock::getQuantity)
                    .orElseThrow(() -> new RuntimeException("재고를 찾을 수 없습니다"));

            // 재고가 음수가 되지 않아야 함
            assertTrue(finalStock >= 0L,
                    String.format("재고가 음수가 되지 않아야 함: 실제 재고=%d", finalStock));
            // 성공한 차감 수만큼 정확히 차감되어야 함
            Long expectedStock = initialStockQuantity - (successCount.get() * decreaseQuantityPerThread);
            assertEquals(expectedStock, finalStock,
                    String.format("성공한 차감 수만큼 정확히 차감되어야 함: 예상 재고=%d, 실제 재고=%d, 성공한 차감 수=%d", expectedStock, finalStock, successCount.get()));
        }
    }
}

