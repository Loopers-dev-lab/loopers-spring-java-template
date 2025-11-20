package com.loopers.application;

import com.loopers.application.like.LikeCommand;
import com.loopers.application.like.UserLikeProductFacade;
import com.loopers.application.order.OrderCommand;
import com.loopers.application.order.UserOrderProductFacade;
import com.loopers.domain.brand.BrandModel;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.brand.BrandStatus;
import com.loopers.domain.like.ProductLikeRepository;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
@SpringBootTest
public class LikeConcurrencyTest {

    @Autowired
    private UserLikeProductFacade userLikeProductFacade;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private ProductLikeRepository productLikeRepository;

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
    @DisplayName("좋아요 동시성 테스트 - 여러 유저가 상품의 좋아요 개수가 정상 반영한다")
    void likeConcurrencyTest() throws InterruptedException {
        // given
        int initStock = 10;
        int orderQty = 1;
        int initPoint = 100_000;
        int productPrice = 5_000;

        int userCount = 10;   // 유저/쓰레드 수
        int threadCount = userCount;

        // 유저 10명 생성
        List<UserModel> users = new ArrayList<>();
        for (int i = 1; i <= userCount; i++) {
            UserModel user = new UserModel(
                    "testuser" + i,
                    "user" + i,
                    "유저테스트" + i,
                    "user" + i + "@test.com",
                    "1997-09-28",
                    "M",
                    initPoint
            );
            users.add(userRepository.save(user));
        }

        BrandModel brand = brandRepository.save(new BrandModel(
                "테스트브랜드",
                "테스트브랜드입니다.",
                BrandStatus.REGISTERED
        ));

        ProductModel product = productRepository.save(new ProductModel(
                "상품B",
                "테스트상품",
                productPrice,
                initStock,
                ProductStatus.ON_SALE,
                brand
        ));

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        // 쓰레드 이름 보기 좋게 설정 (선택)
        ExecutorService executor = Executors.newFixedThreadPool(
                threadCount,
                r -> {
                    Thread t = new Thread(r);
                    t.setName("order-test-" + t.getId());
                    return t;
                }
        );

        // 10개의 쓰레드에 각각 다른 유저를 할당해서 상품 좋아요
        for (int i = 0; i < threadCount; i++) {
            final String userId = users.get(i).getUserId();

            executor.submit(() -> {
                try {
                    startLatch.await();

                    LikeCommand.Like input = new LikeCommand.Like(
                      userId,
                      product.getId()
                    );
                    userLikeProductFacade.userLikeProduct(input);

                    successCount.incrementAndGet();
                } catch (CoreException e) {
                    // 재고 부족, 포인트 부족 등의 비즈니스 예외
                    log.error("[thread={}] 비즈니스 에러 발생: {}",
                            Thread.currentThread().getName(), e.getMessage(), e);
                    failCount.incrementAndGet();
                } catch (Exception e) {
                    // 그 외 예외
                    log.error("[thread={}] 시스템 에러 발생: {}",
                            Thread.currentThread().getName(), e.getMessage(), e);
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // when
        startLatch.countDown();  // 모든 쓰레드 동시에 출발
        doneLatch.await();       // 전부 끝날 때까지 대기
        executor.shutdown();

        // then
        int reloaded_count = productLikeRepository.countByProductId(product.getId());
        int expectedLikeCount =  users.size();
        assertThat(reloaded_count).isEqualTo(expectedLikeCount);
        assertThat(successCount.get()).isEqualTo(users.size());


    }

}
