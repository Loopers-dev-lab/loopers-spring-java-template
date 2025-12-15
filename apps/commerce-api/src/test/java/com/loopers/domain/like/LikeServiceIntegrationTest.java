package com.loopers.domain.like;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Product;
import com.loopers.domain.user.User;
import com.loopers.fixture.TestFixture;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
class LikeServiceIntegrationTest {

    @Autowired
    private LikeService likeService;

    @MockitoSpyBean
    private LikeRepository likeRepository;

    @Autowired
    private TestFixture testFixture;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private User user1, user2;
    private Product product1, product2;

    @BeforeEach
    void setUp() {
        user1 = testFixture.createUser("likeUser01");
        user2 = testFixture.createUser("likeUser02");

        Brand brand = testFixture.createBrand("Like Brand");
        product1 = testFixture.createProduct("Like Product 1", 1000L, 10, brand);
        product2 = testFixture.createProduct("Like Product 2", 2000L, 20, brand);
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("좋아요 등록/취소/중복 방지")
    @Nested
    class CoreLikeFlow {

        @DisplayName("좋아요를 등록할 수 있다.")
        @Test
        void addLike() {
            // act
            likeService.addLike(user1, product1);

            // assert
            verify(likeRepository, times(1)).save(any(Like.class));
            assertThat(likeRepository.existsByUserAndProduct(user1, product1)).isTrue();
        }

        @DisplayName("중복 좋아요 방지를 위한 멱등성 처리가 구현되었다.")
        @Test
        void addLike_idempotent() {
            // arrange
            likeService.addLike(user1, product1);

            // act
            likeService.addLike(user1, product1);

            // assert
            verify(likeRepository, times(1)).save(any(Like.class));
            verify(likeRepository, times(2)).existsByUserAndProduct(user1, product1);
        }

        @DisplayName("좋아요를 취소할 수 있다.")
        @Test
        void removeLike() {
            likeService.addLike(user1, product1);
            assertThat(likeRepository.existsByUserAndProduct(user1, product1)).isTrue();

            likeService.removeLike(user1, product1);

            assertThat(likeRepository.existsByUserAndProduct(user1, product1)).isFalse();
        }

        @DisplayName("좋아요를 누르지 않은 상품을 취소해도 에러가 발생하지 않는다.")
        @Test
        void removeLike_nonExistent() {
            // arrange
            assertThat(likeRepository.existsByUserAndProduct(user1, product1)).isFalse();

            // act
            likeService.removeLike(user1, product1);

            // assert
            verify(likeRepository, never()).delete(any(Like.class));
        }
    }

    @DisplayName("좋아요 수 조회")
    @Nested
    class GetLikeCount {

        @DisplayName("특정 상품의 좋아요 수를 조회할 수 있다.")
        @Test
        void getLikeCount() {
            // arrange
            likeService.addLike(user1, product1); // product1 (1)
            likeService.addLike(user2, product1); // product1 (2)
            likeService.addLike(user1, product2); // product2 (1)

            // act
            Long count1 = likeService.getLikeCount(product1);
            Long count2 = likeService.getLikeCount(product2);

            // assert
            verify(likeRepository, times(1)).countByProduct(product1);
            verify(likeRepository, times(1)).countByProduct(product2);
            assertThat(count1).isEqualTo(2L);
            assertThat(count2).isEqualTo(1L);
        }
    }
}
