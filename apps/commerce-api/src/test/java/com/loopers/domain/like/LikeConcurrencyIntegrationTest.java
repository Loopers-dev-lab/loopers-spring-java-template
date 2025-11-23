package com.loopers.domain.like;

import com.loopers.application.like.LikeFacade;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.UserEntity;
import com.loopers.domain.user.UserRepository;
import com.loopers.interfaces.api.like.LikeV1Dto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class LikeConcurrencyIntegrationTest {

    @Autowired
    private LikeFacade likeFacade;

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void 동시에_100개의_좋아요요청이_들어오면_좋아요는_1개만_생성된다() throws Exception {

        // 1. 유저/상품 테스트 데이터 생성
        UserEntity user = userRepository.save(
                new UserEntity(
                        "happy97",
                        "test@test.com",
                        Gender.MALE,
                        "1997-09-23",
                        "test1234!"
                )
        );

        Product product = productRepository.save(
                new Product(
                        1L,
                        "테스트 상품",
                        BigDecimal.valueOf(10000),
                        100
                )
        );

        Long userId = user.getId();
        Long productId = product.getId();

        // 2. 동시 실행 환경 설정
        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // 3. 동시에 doLike() 실행
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    likeFacade.doLike(new LikeV1Dto.LikeRequest(userId, productId));
                } catch (Exception e) {
                    // 스레드 예외 출력 (중요)
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        // 4. 모든 요청이 끝날 때까지 대기
        latch.await();

        // 5. 좋아요 개수 확인
        long count = likeRepository.countByUserIdAndProductId(userId, productId);

        assertThat(count).isEqualTo(1);
    }
}
