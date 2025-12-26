package com.loopers.domain.like;

import com.loopers.application.like.LikeFacade;
import com.loopers.application.members.MemberFacade;
import com.loopers.application.members.MemberInfo;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.repository.BrandRepository;
import com.loopers.domain.common.vo.Money;
import com.loopers.domain.like.repository.LikeRepository;
import com.loopers.domain.members.enums.Gender;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.repository.ProductRepository;
import com.loopers.domain.product.vo.Stock;
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
@DisplayName("좋아요 동시성 테스트")
class LikeConcurrencyTest {

    @Autowired
    private LikeFacade likeFacade;

    @Autowired
    private MemberFacade memberFacade;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @Test
    @DisplayName("동일한 상품에 대해 여러 명이 동시에 좋아요를 요청해도, 좋아요 개수가 정상 반영되어야 한다")
    void shouldHandleConcurrentLikes_whenMultipleUsersLikeSameProduct() throws InterruptedException {
        // given
        Brand brand = brandRepository.save(new Brand("TestBrand", "Test Brand Description"));
        Product product = new Product(
                brand.getId(),
                "Test Product",
                "Test Description",
                Money.of(BigDecimal.valueOf(10000)),
                Stock.of(100)
        );
        Product savedProduct = productRepository.save(product);
        Long productId = savedProduct.getId();

        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);

        // 10명의 회원 생성
        List<Long> memberIds = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            String memberId = "member" + i;
            MemberInfo member = memberFacade.registerMember(memberId, memberId + "@test.com", "password", "1990-01-01", Gender.MALE);
            memberIds.add(member.id());
        }

        // when: 10명이 동시에 좋아요
        try (ExecutorService executorService = Executors.newFixedThreadPool(threadCount)) {
            for (int i = 0; i < threadCount; i++) {
                final Long memberId = memberIds.get(i);
                executorService.submit(() -> {
                    try {
                        likeFacade.likeProduct(memberId, productId);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
        }

        // then
        long likeCount = likeRepository.countByProductId(productId);
        assertThat(likeCount).isEqualTo(threadCount);
    }

    @Test
    @DisplayName("동일한 상품에 대해 여러 명이 동시에 좋아요 취소를 요청해도, 좋아요 개수가 정상 반영되어야 한다")
    void shouldHandleConcurrentUnlikes_whenMultipleUsersUnlikeSameProduct() throws InterruptedException {
        // given
        Brand brand = brandRepository.save(new Brand("TestBrand", "Test Brand Description"));
        Product product = new Product(
                brand.getId(),
                "Test Product",
                "Test Description",
                Money.of(BigDecimal.valueOf(10000)),
                Stock.of(100)
        );
        Product savedProduct = productRepository.save(product);
        Long productId = savedProduct.getId();

        int threadCount = 10;

        // 10명의 회원 생성 및 미리 좋아요
        List<Long> memberIds = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            String memberId = "member" + i;
            MemberInfo member = memberFacade.registerMember(memberId, memberId + "@test.com", "password", "1990-01-01", Gender.MALE);
            memberIds.add(member.id());
            likeFacade.likeProduct(member.id(), productId);
        }

        // 좋아요 개수 확인
        long beforeUnlikeCount = likeRepository.countByProductId(productId);
        assertThat(beforeUnlikeCount).isEqualTo(threadCount);

        CountDownLatch latch = new CountDownLatch(threadCount);

        // when: 10명이 동시에 좋아요 취소
        try (ExecutorService executorService = Executors.newFixedThreadPool(threadCount)) {
            for (int i = 0; i < threadCount; i++) {
                final Long memberId = memberIds.get(i);
                executorService.submit(() -> {
                    try {
                        likeFacade.unlikeProduct(memberId, productId);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
        }

        // then
        long likeCount = likeRepository.countByProductId(productId);
        assertThat(likeCount).isEqualTo(0);
    }

    @Test
    @DisplayName("동일한 상품에 대해 여러 명이 동시에 좋아요/취소를 섞어서 요청해도, 좋아요 개수가 정상 반영되어야 한다")
    void shouldHandleConcurrentMixedLikes_whenMultipleUsersLikeAndUnlikeSameProduct() throws InterruptedException {
        // given
        Brand brand = brandRepository.save(new Brand("TestBrand", "Test Brand Description"));
        Product product = new Product(
                brand.getId(),
                "Test Product",
                "Test Description",
                Money.of(BigDecimal.valueOf(10000)),
                Stock.of(100)
        );
        Product savedProduct = productRepository.save(product);
        Long productId = savedProduct.getId();

        int likeCount = 10;
        int unlikeCount = 5;
        int totalThreadCount = likeCount + unlikeCount;

        // 회원 생성
        List<Long> memberIds = new ArrayList<>();
        for (int i = 0; i < totalThreadCount; i++) {
            String memberId = "member" + i;
            MemberInfo member = memberFacade.registerMember(memberId, memberId + "@test.com", "password", "1990-01-01", Gender.MALE);
            memberIds.add(member.id());
        }

        // unlike할 회원들은 미리 좋아요
        for (int i = likeCount; i < totalThreadCount; i++) {
            likeFacade.likeProduct(memberIds.get(i), productId);
        }

        CountDownLatch latch = new CountDownLatch(totalThreadCount);

        // when: 10명은 좋아요, 5명은 취소
        try (ExecutorService executorService = Executors.newFixedThreadPool(totalThreadCount)) {
            for (int i = 0; i < likeCount; i++) {
                final Long memberId = memberIds.get(i);
                executorService.submit(() -> {
                    try {
                        likeFacade.likeProduct(memberId, productId);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            for (int i = likeCount; i < totalThreadCount; i++) {
                final Long memberId = memberIds.get(i);
                executorService.submit(() -> {
                    try {
                        likeFacade.unlikeProduct(memberId, productId);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
        }

        // then: likeCount명만 좋아요 상태여야 함
        long finalLikeCount = likeRepository.countByProductId(productId);
        assertThat(finalLikeCount).isEqualTo(likeCount);
    }
}
