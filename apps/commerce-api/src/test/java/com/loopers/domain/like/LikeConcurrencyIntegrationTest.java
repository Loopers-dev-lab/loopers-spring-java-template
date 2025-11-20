package com.loopers.domain.like;

import com.loopers.application.like.LikeFacade;
import com.loopers.interfaces.api.like.LikeV1Dto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class LikeConcurrencyIntegrationTest {

    @Autowired
    private LikeFacade likeFacade;

    @Autowired
    private LikeRepository likeRepository;

    @Test
    void 동시에_100개의_좋아요요청이_들어오면_좋아요는_1개만_생성된다() throws Exception {
        Long userId = 1L;
        Long productId = 1L;

        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    likeFacade.doLike(new LikeV1Dto.LikeRequest(userId, productId));
                } finally {
                    latch.countDown();
                }
            });
        }

        // 모든 스레드 작업이 종료될 때까지 대기
        latch.await();

        long count = likeRepository.countByUserIdAndProductId(userId, productId);
        assertThat(count).isEqualTo(1);
    }
}
