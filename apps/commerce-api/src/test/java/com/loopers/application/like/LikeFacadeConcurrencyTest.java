package com.loopers.application.like;

import static org.assertj.core.api.Assertions.assertThat;

import com.loopers.application.user.UserCommand;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.product.LikeCount;
import com.loopers.domain.product.Price;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.Stock;
import com.loopers.domain.user.UserService;
import com.loopers.utils.DatabaseCleanUp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
class LikeFacadeConcurrencyTest {

    @Autowired
    private LikeLockFacade likeLockFacade;

    @Autowired
    private UserService userService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductService productService;

    @MockitoSpyBean
    private BrandService brandService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private Long productId;
    private List<String> userLoginIds;
    private int initialLikeCount;

    @BeforeEach
    void setUp() {
        Product product = Product.createProduct(
                "상품",
                1L,
                Price.createPrice(10000),
                LikeCount.createLikeCount(0),
                Stock.createStock(100)
        );
        productRepository.saveProduct(product);
        productId = product.getId();
        initialLikeCount = 0;

        userLoginIds = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            String loginId = "user" + i;
            UserCommand.SignupCommand signupCommand = new UserCommand.SignupCommand(
                    loginId,
                    loginId + "@test.com",
                    "2000-01-01",
                    "M"
            );
            userService.signup(signupCommand);
            userLoginIds.add(loginId);
        }
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("동일한 상품에 대해 여러 사용자가 동시에 좋아요를 등록하면, 좋아요 수가 정확히 반영되어야 한다.")
    @Test
    void shouldReflectLikeCountCorrectly_whenMultipleUsersRecordLikeConcurrently() throws Exception {
        // arrange
        int concurrentRequestCount = 4;
        ExecutorService executorService = Executors.newFixedThreadPool(concurrentRequestCount);
        CountDownLatch latch = new CountDownLatch(concurrentRequestCount);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        // act
        for (String loginId : userLoginIds) {
            executorService.submit(() -> {
                try {
                    latch.countDown();
                    latch.await();

                    LikeCommand.LikeProductCommand command = new LikeCommand.LikeProductCommand(
                            loginId,
                            productId
                    );

                    likeLockFacade.recordLike(command);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                }
            });
        }

        executorService.shutdown();
        if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
            executorService.shutdownNow();
        }

        // assert
        assertThat(successCount.get()).isEqualTo(concurrentRequestCount);
        assertThat(failCount.get()).isEqualTo(0);

        Product finalProduct = productService.findProductById(productId).orElseThrow();
        assertThat(finalProduct.getLikeCount().getCount()).isEqualTo(initialLikeCount + concurrentRequestCount);
    }

    @DisplayName("동일한 상품에 대해 여러 사용자가 동시에 좋아요를 취소하면, 좋아요 수가 정확히 반영되어야 한다.")
    @Test
    void shouldReflectLikeCountCorrectly_whenMultipleUsersCancelLikeConcurrently() throws Exception {
        // arrange
        for (String loginId : userLoginIds) {
            LikeCommand.LikeProductCommand command = new LikeCommand.LikeProductCommand(
                    loginId,
                    productId
            );
            likeLockFacade.recordLike(command);
        }

        int concurrentRequestCount = 4;
        ExecutorService executorService = Executors.newFixedThreadPool(concurrentRequestCount);
        CountDownLatch latch = new CountDownLatch(concurrentRequestCount);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        // act
        for (String loginId : userLoginIds) {
            executorService.submit(() -> {
                try {
                    latch.countDown();
                    latch.await();

                    LikeCommand.LikeProductCommand command = new LikeCommand.LikeProductCommand(
                            loginId,
                            productId
                    );

                    likeLockFacade.cancelLike(command);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                }
            });
        }

        executorService.shutdown();
        if (!executorService.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
            executorService.shutdownNow();
        }

        // assert
        assertThat(successCount.get()).isEqualTo(concurrentRequestCount);
        assertThat(failCount.get()).isEqualTo(0);

        Product finalProduct = productService.findProductById(productId).orElseThrow();
        assertThat(finalProduct.getLikeCount().getCount()).isEqualTo(0);
    }

    @DisplayName("동일한 상품에 대해 일부는 좋아요 등록, 일부는 좋아요 취소를 동시에 요청하면, 좋아요 수가 정확히 반영되어야 한다.")
    @Test
    void shouldReflectLikeCountCorrectly_whenRecordAndCancelLikeConcurrently() throws Exception {
        // arrange
        int halfCount = userLoginIds.size() / 2;
        for (int i = 0; i < halfCount; i++) {
            LikeCommand.LikeProductCommand command = new LikeCommand.LikeProductCommand(
                    userLoginIds.get(i),
                    productId
            );
            likeLockFacade.recordLike(command);
        }

        int concurrentRequestCount = userLoginIds.size();
        ExecutorService executorService = Executors.newFixedThreadPool(concurrentRequestCount);
        CountDownLatch latch = new CountDownLatch(concurrentRequestCount);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        // act
        for (int i = 0; i < userLoginIds.size(); i++) {
            final int index = i;
            final String loginId = userLoginIds.get(i);

            executorService.submit(() -> {
                try {
                    latch.countDown();
                    latch.await();

                    LikeCommand.LikeProductCommand command = new LikeCommand.LikeProductCommand(
                            loginId,
                            productId
                    );

                    if (index < halfCount) {
                        likeLockFacade.cancelLike(command);
                    } else {
                        likeLockFacade.recordLike(command);
                    }
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                }
            });
        }

        executorService.shutdown();
        if (!executorService.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
            executorService.shutdownNow();
        }

        // assert
        assertThat(successCount.get()).isEqualTo(concurrentRequestCount);
        assertThat(failCount.get()).isEqualTo(0);

        Product finalProduct = productService.findProductById(productId).orElseThrow();
        assertThat(finalProduct.getLikeCount().getCount()).isEqualTo(halfCount);
    }
}

