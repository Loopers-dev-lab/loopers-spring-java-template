package com.loopers.application.event.integration;

import com.loopers.application.event.like.ProductLikedEvent;
import com.loopers.application.event.like.ProductUnlikedEvent;
import com.loopers.domain.common.vo.Money;
import com.loopers.domain.like.Like;
import com.loopers.domain.like.repository.LikeRepository;
import com.loopers.domain.like.service.LikeService;
import com.loopers.domain.members.Member;
import com.loopers.domain.members.enums.Gender;
import com.loopers.domain.members.repository.MemberRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.repository.ProductRepository;
import com.loopers.domain.product.vo.Stock;
import com.loopers.infrastructure.cache.MemberLikesCache;
import com.loopers.infrastructure.cache.ProductLikeCountCache;
import com.loopers.utils.DatabaseCleanUp;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * 좋아요 이벤트 플로우 통합 테스트
 *
 * 검증 항목:
 * 1. 좋아요 생성 시 이벤트 발행 및 Redis 캐시 업데이트
 * 2. 좋아요 취소 시 이벤트 발행 및 Redis 캐시 업데이트
 * 3. Redis와 DB의 최종 일관성 (eventual consistency)
 * 4. 여러 좋아요/취소 작업 후 데이터 일치 확인
 */
@SpringBootTest
@RecordApplicationEvents
@DisplayName("좋아요 이벤트 통합 테스트")
class LikeEventIntegrationTest {

    @Autowired
    private LikeService likeService;

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProductLikeCountCache productLikeCountCache;

    @Autowired
    private MemberLikesCache memberLikesCache;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ApplicationEvents applicationEvents;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private Member testMember;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        // Redis 초기화
        redisTemplate.getConnectionFactory().getConnection().flushDb();

        // 테스트 회원 생성
        testMember = new Member("like-user", "like@example.com", "password123", "1995-05-05", Gender.FEMALE);
        testMember = memberRepository.save(testMember);

        // 테스트 상품 생성
        testProduct = new Product(10L, "인기 상품", "좋아요 테스트용", Money.of(50000), Stock.of(50));
        testProduct = productRepository.save(testProduct);

        entityManager.flush();
        entityManager.clear();
    }

    @AfterEach
    void tearDown() {
        redisTemplate.getConnectionFactory().getConnection().flushDb();
        databaseCleanUp.truncateAllTables();
    }

    @Test
    @DisplayName("좋아요 생성 시 ProductLikedEvent 발행되고 Redis 캐시가 업데이트된다")
    void 좋아요_생성_이벤트_및_Redis_업데이트() {
        // given
        Long memberId = testMember.getId();
        Long productId = testProduct.getId();

        // when
        likeService.like(memberId, productId);

        // then - DB에 Like 레코드 저장 확인
        entityManager.clear();
        Like savedLike = likeRepository.findByMemberIdAndProductId(memberId, productId).orElse(null);
        assertThat(savedLike).isNotNull();
        assertThat(savedLike.getMemberId()).isEqualTo(memberId);
        assertThat(savedLike.getProductId()).isEqualTo(productId);

        // then - ProductLikedEvent 발행 확인
        long likedEventCount = applicationEvents.stream(ProductLikedEvent.class)
            .filter(event -> event.memberId().equals(memberId) && event.productId().equals(productId))
            .count();
        assertThat(likedEventCount).isGreaterThan(0);

        // then - 비동기 이벤트 처리 후 Redis 업데이트 확인
        await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
            // Redis 좋아요 카운트 증가 확인
            Long redisCount = productLikeCountCache.get(productId);
            assertThat(redisCount).isEqualTo(1L);

            // Redis 회원 좋아요 목록 추가 확인
            boolean isMemberLiked = memberLikesCache.isMember(memberId, productId);
            assertThat(isMemberLiked).isTrue();
        });
    }

    @Test
    @DisplayName("좋아요 취소 시 ProductUnlikedEvent 발행되고 Redis 캐시가 업데이트된다")
    void 좋아요_취소_이벤트_및_Redis_업데이트() {
        // given - 먼저 좋아요 생성
        Long memberId = testMember.getId();
        Long productId = testProduct.getId();
        likeService.like(memberId, productId);

        // 비동기 처리 대기
        await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(productLikeCountCache.get(productId)).isEqualTo(1L);
        });

        entityManager.clear();

        // when - 좋아요 취소
        likeService.unlike(memberId, productId);

        // then - DB에서 Like 레코드 삭제 확인
        entityManager.clear();
        Like deletedLike = likeRepository.findByMemberIdAndProductId(memberId, productId).orElse(null);
        assertThat(deletedLike).isNull();

        // then - ProductUnlikedEvent 발행 확인
        long unlikedEventCount = applicationEvents.stream(ProductUnlikedEvent.class)
            .filter(event -> event.memberId().equals(memberId) && event.productId().equals(productId))
            .count();
        assertThat(unlikedEventCount).isGreaterThan(0);

        // then - 비동기 이벤트 처리 후 Redis 업데이트 확인
        await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
            // Redis 좋아요 카운트 감소 확인
            Long redisCount = productLikeCountCache.get(productId);
            assertThat(redisCount).isEqualTo(0L);

            // Redis 회원 좋아요 목록 제거 확인
            boolean isMemberLiked = memberLikesCache.isMember(memberId, productId);
            assertThat(isMemberLiked).isFalse();
        });
    }

    @Test
    @DisplayName("여러 회원이 동일 상품에 좋아요 시 Redis 카운트가 정확히 증가한다")
    void 여러_회원_좋아요_Redis_카운트_증가() {
        // given - 추가 회원 생성
        Member member2 = new Member("user2", "user2@example.com", "password123", "1992-02-02", Gender.MALE);
        member2 = memberRepository.save(member2);

        Member member3 = new Member("user3", "user3@example.com", "password123", "1993-03-03", Gender.FEMALE);
        member3 = memberRepository.save(member3);

        Long productId = testProduct.getId();

        // when - 3명이 좋아요
        likeService.like(testMember.getId(), productId);
        likeService.like(member2.getId(), productId);
        likeService.like(member3.getId(), productId);

        // then - DB에 3개 레코드 저장 확인
        entityManager.clear();
        long dbLikeCount = likeRepository.countByProductId(productId);
        assertThat(dbLikeCount).isEqualTo(3L);

        // then - 비동기 처리 후 Redis 카운트 3 확인
        await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
            Long redisCount = productLikeCountCache.get(productId);
            assertThat(redisCount).isEqualTo(3L);
        });
    }

    @Test
    @DisplayName("Redis와 DB의 최종 일관성 확인 - 여러 좋아요/취소 작업 후")
    void Redis_DB_최종_일관성_확인() {
        // given - 추가 회원들 생성
        Member member2 = memberRepository.save(new Member("u2", "u2@test.com", "pw", "1990-01-01", Gender.MALE));
        Member member3 = memberRepository.save(new Member("u3", "u3@test.com", "pw", "1990-01-01", Gender.FEMALE));
        Member member4 = memberRepository.save(new Member("u4", "u4@test.com", "pw", "1990-01-01", Gender.MALE));

        Long productId = testProduct.getId();

        // when - 복잡한 좋아요/취소 시나리오
        likeService.like(testMember.getId(), productId);  // +1 = 1
        likeService.like(member2.getId(), productId);      // +1 = 2
        likeService.like(member3.getId(), productId);      // +1 = 3
        likeService.unlike(member2.getId(), productId);    // -1 = 2
        likeService.like(member4.getId(), productId);      // +1 = 3
        likeService.unlike(testMember.getId(), productId); // -1 = 2

        // 비동기 처리 완료 대기
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            entityManager.clear();

            // then - DB 카운트 확인 (member3, member4만 남음 = 2)
            long dbCount = likeRepository.countByProductId(productId);
            assertThat(dbCount).isEqualTo(2L);

            // then - Redis 카운트 확인 (DB와 일치)
            Long redisCount = productLikeCountCache.get(productId);
            assertThat(redisCount).isEqualTo(2L);

            // then - 회원별 좋아요 여부 확인
            assertThat(memberLikesCache.isMember(testMember.getId(), productId)).isFalse();
            assertThat(memberLikesCache.isMember(member2.getId(), productId)).isFalse();
            assertThat(memberLikesCache.isMember(member3.getId(), productId)).isTrue();
            assertThat(memberLikesCache.isMember(member4.getId(), productId)).isTrue();
        });
    }

    @Test
    @DisplayName("중복 좋아요 시도 시 DB는 중복 저장 안 되고 이벤트도 1회만 발행")
    void 중복_좋아요_멱등성() {
        // given
        Long memberId = testMember.getId();
        Long productId = testProduct.getId();

        // when - 동일 회원이 같은 상품에 3번 좋아요
        likeService.like(memberId, productId);
        likeService.like(memberId, productId);
        likeService.like(memberId, productId);

        // then - DB에는 1개만 저장
        entityManager.clear();
        boolean exists = likeRepository.existsByMemberIdAndProductId(memberId, productId);
        assertThat(exists).isTrue();

        // then - 비동기 처리 후 Redis 카운트도 1
        await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
            Long redisCount = productLikeCountCache.get(productId);
            assertThat(redisCount).isEqualTo(1L);
        });
    }
}
