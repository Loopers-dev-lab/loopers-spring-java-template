package com.loopers.domain.like;

import com.loopers.domain.common.vo.Money;
import com.loopers.domain.like.repository.LikeRepository;
import com.loopers.domain.like.service.LikeService;
import com.loopers.domain.members.Member;
import com.loopers.domain.members.enums.Gender;
import com.loopers.domain.members.repository.MemberRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.repository.ProductRepository;
import com.loopers.domain.product.vo.Stock;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.*;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class LikeServiceIntegrationTest {

    @Autowired
    private LikeService likeService;

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private DatabaseCleanUp cleanUp;

    @Autowired
    private EntityManager entityManager;

    @AfterEach
    void tearDown() {
        cleanUp.truncateAllTables();
    }

    private Product createProduct(Long brandId, String name, long price, int stock) {
        return new Product(brandId, name, null, Money.of(price), Stock.of(stock));
    }

    private Member createMember(String memberId, String email) {
        return new Member(memberId, email, "password123", "1990-01-01", Gender.MALE);
    }

    @Nested
    @DisplayName("좋아요 기능 통합 테스트")
    class LikeTests {

        @Test
        @DisplayName("좋아요 생성 성공 → 좋아요 저장 + 상품의 likeCount 증가")
        @Transactional
        void likeSuccess() {
            // given
            Member member = memberRepository.save(createMember("user1", "u1@mail.com"));
            Product product = productRepository.save(createProduct(1L, "상품A", 1000L, 10));

            // when
            likeService.like(member.getMemberId(), product.getId());

            // then
            Like saved = likeRepository.findByMemberIdAndProductId("user1", product.getId()).orElse(null);
            assertThat(saved).isNotNull();

            entityManager.clear(); // 1차 캐시 클리어
            Product updated = productRepository.findById(product.getId()).get();
            assertThat(updated.getLikeCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("중복 좋아요 시 likeCount 증가 안 하고 저장도 안 됨")
        @Transactional
        void duplicateLike() {
            // given
            memberRepository.save(createMember("user1", "u1@mail.com"));
            Product product = productRepository.save(createProduct(1L, "상품A", 1000L, 10));

            likeService.like("user1", product.getId());

            // when
            likeService.like("user1", product.getId()); // 중복 호출

            // then
            long likeCount = likeRepository.countByProductId(product.getId());
            assertThat(likeCount).isEqualTo(1L);

            entityManager.clear(); // 1차 캐시 클리어
            Product updated = productRepository.findById(product.getId()).get();
            assertThat(updated.getLikeCount()).isEqualTo(1); // 증가 X
        }

        @Test
        @DisplayName("좋아요 취소 성공 → like 삭제 + 상품의 likeCount 감소")
        @Transactional
        void unlikeSuccess() {
            // given
            memberRepository.save(createMember("user1", "u1@mail.com"));
            Product product = productRepository.save(createProduct(1L, "상품A", 1000L, 10));

            likeService.like("user1", product.getId());

            // when
            likeService.unlike("user1", product.getId());

            // then
            Like like = likeRepository.findByMemberIdAndProductId("user1", product.getId()).orElse(null);
            assertThat(like).isNull();

            Product updated = productRepository.findById(product.getId()).get();
            assertThat(updated.getLikeCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("없는 좋아요 취소 시 likeCount 감소 안 함")
        @Transactional
        void unlikeNonExisting() {
            // given
            memberRepository.save(createMember("user1", "u1@mail.com"));
            Product product = createProduct(1L, "상품A", 1000L, 10);
            product.increaseLikeCount();
            product.increaseLikeCount();
            product.increaseLikeCount();
            product.increaseLikeCount();
            product.increaseLikeCount();

            product = productRepository.save(product);

            // when — 호출은 해도
            likeService.unlike("user1", product.getId());

            // then — 변화 없음
            Product updated = productRepository.findById(product.getId()).get();
            assertThat(updated.getLikeCount()).isEqualTo(5);
        }

        @Test
        @DisplayName("countByProductId 정상 조회")
        @Transactional
        void countTest() {
            // given
            memberRepository.save(createMember("user1", "u1@mail.com"));
            memberRepository.save(createMember("user2", "u2@mail.com"));
            Product product = productRepository.save(createProduct(1L, "상품A", 1000L, 10));

            likeService.like("user1", product.getId());
            likeService.like("user2", product.getId());

            // when
            long count = likeRepository.countByProductId(product.getId());

            // then
            assertThat(count).isEqualTo(2L);
        }
    }
}
