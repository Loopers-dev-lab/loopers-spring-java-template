package com.loopers.domain.like;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
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
    private UserService userService;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private User user1, user2;
    private Product product1, product2;

    @BeforeEach
    void setUp() {
        // 테스트용 사용자 생성
        user1 = userService.signUp("likeUser01", "like@mail.com", "1990-01-01", Gender.MALE);
        user2 = userService.signUp("likeUser02", "like@mail.com", "1990-01-02", Gender.FEMALE);

        // 테스트용 상품 생성
        Brand brand = brandRepository.save(Brand.create("Like Brand"));
        product1 = productRepository.save(Product.create("Like Product 1", 1000L, 10, brand));
        product2 = productRepository.save(Product.create("Like Product 2", 2000L, 20, brand));
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
            likeService.addLike(user1, product1); // 1번째 호출

            // act
            likeService.addLike(user1, product1); // 2번째 (중복) 호출

            // assert
            // 1. Service 로직(existsByUserAndProduct)에 의해 save는 총 1번만 호출되어야 함
            verify(likeRepository, times(1)).save(any(Like.class));
            // 2. existsByUserAndProduct는 총 2번 호출됨 (첫번째 시도 + 두번째 중복 체크)
            verify(likeRepository, times(2)).existsByUserAndProduct(user1, product1);
        }

        @DisplayName("좋아요를 취소할 수 있다.")
        @Test
        void removeLike() {
            // arrange (미리 좋아요 추가)
            likeService.addLike(user1, product1);
            assertThat(likeRepository.existsByUserAndProduct(user1, product1)).isTrue();

            // act
            likeService.removeLike(user1, product1);

            // assert
            verify(likeRepository, times(1)).delete(any(Like.class));
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
