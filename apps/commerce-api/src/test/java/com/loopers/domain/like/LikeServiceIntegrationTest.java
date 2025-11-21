package com.loopers.domain.like;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.order.Money;
import com.loopers.domain.product.Product;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserFixture;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.like.LikeJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.List;
import java.util.Optional;

import static com.loopers.domain.like.LikeAssertions.assertLike;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


@SpringBootTest
class LikeServiceIntegrationTest {
  @Autowired
  private LikeService likeService;

  @MockitoSpyBean
  private LikeJpaRepository likeJpaRepository;
  @MockitoSpyBean
  private UserJpaRepository userJpaRepository;
  @MockitoSpyBean
  private BrandJpaRepository brandJpaRepository;
  @MockitoSpyBean
  private ProductJpaRepository productJpaRepository;

  @Autowired
  private DatabaseCleanUp databaseCleanUp;

  User savedUser;
  Brand savedBrand;
  Product savedProduct;

  @BeforeEach
  void setup() {
    savedUser = userJpaRepository.save(UserFixture.createUser());
    Brand brand = Brand.create("레이브", "레이브는 음악, 영화, 예술 등 다양한 문화에서 영감을 받아 경계 없고 자유분방한 스타일을 제안하는 패션 레이블입니다.");
    savedBrand = brandJpaRepository.save(brand);
    Product product = Product.create(savedBrand, "Wild Faith Rose Sweatshirt", Money.wons(80_000));
    savedProduct = productJpaRepository.save(product);
  }

  @AfterEach
  void tearDown() {
    databaseCleanUp.truncateAllTables();
  }

  @DisplayName("좋아요를 조회할 때,")
  @Nested
  class Create {
    @DisplayName("존재하는 좋아요 ID를 주면, 좋아요 성공한다.")
    @Test
    void 성공_존재하는_좋아요ID() {
      // arrange
      Long userId = savedUser.getId();
      Long productId = savedProduct.getId();
      // act
      likeService.save(userId, productId);
      Optional<Like> savedLike = likeJpaRepository.findByUserIdAndProductId(userId, productId);
      // assert
      assertThat(savedLike).isPresent();
      assertThat(savedLike.get().getUser().getId()).isEqualTo(userId);
      assertThat(savedLike.get().getProduct().getId()).isEqualTo(productId);

    }

    @DisplayName("동일한 좋아요를 중복 저장해도 일관된 결과를 반환한다")
    @Test
    void 성공_이미_존재하는_좋아요ID() {
      // arrange
      Long userId = savedUser.getId();
      Long productId = savedProduct.getId();

      likeService.save(userId, productId);
      Optional<Like> result1 = likeJpaRepository.findByUserIdAndProductId(userId, productId);

      // act
      likeService.save(userId, productId);
      Optional<Like> result2 = likeJpaRepository.findByUserIdAndProductId(userId, productId);

      // assert
      assertThat(result1).isPresent();
      assertThat(result2).isPresent();
      assertLike(result1.get(), result2.get());
    }
  }

  @DisplayName("좋아요를 삭제할 때,")
  @Nested
  class Delete {
    @DisplayName("존재하는 좋아요를 삭제한다.")
    @Test
    void 성공_존재하는_좋아요ID() {
      // arrange
      Long userId = savedUser.getId();
      Long productId = savedProduct.getId();

      likeService.save(userId, productId);
      Like like = likeJpaRepository.findByUserIdAndProductId(userId, productId).orElseThrow();

      // act
      likeService.remove(savedUser.getId(), savedProduct.getId());

      // assert
      verify(likeJpaRepository, times(1)).delete(userId, productId);
      //assertThrows(NotFoundException.class, () -> service.read(id));
    }

    @DisplayName("존재하지 않는 좋아요 ID를 삭제하면, 예외가 발생하지 않는다.")
    @Test
    void 성공_이미_삭제된_좋아요() {
      // arrange
      Long userId = savedUser.getId();
      Long productId = savedProduct.getId();

      likeService.save(userId, productId);
      likeService.remove(savedUser.getId(), savedProduct.getId());
      likeJpaRepository.flush(); // 즉시 삭제 반영
      // act

      // assert
      assertThatCode(() -> likeService.remove(userId, productId))
          .doesNotThrowAnyException();
    }
  }

  @DisplayName("좋아요 상품목록을 조회할 때,")
  @Nested
  class GetList {
    @DisplayName("페이징 처리되어, 초기설정시 size=20, sort=최신순으로 목록이 조회된다.")
    @Test
    void 성공_좋아요_상품목록조회() {
      // arrange
      Long userId = savedUser.getId();
      Long productId = savedProduct.getId();

      likeService.save(userId, productId);

      // act
      Page<Product> productsPage = likeService.getLikedProducts(savedUser.getId(), "latest", 0, 20);
      List<Product> products = productsPage.getContent();

      // assert
      assertThat(products).isNotEmpty().hasSize(1);
    }

  }
}
