package com.loopers.domain.like;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.order.Money;
import com.loopers.domain.product.Product;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserFixture;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.List;

import static com.loopers.domain.like.LikeAssertions.assertLike;
import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
class LikeServiceTest {

  private final LikeService likeService;

  @Autowired
  public LikeServiceTest(LikeService likeService) {
    this.likeService = likeService;
  }

  @TestConfiguration
  static class FakeRepositoryConfig {

    @Bean
    public LikeRepository likeRepository() {
      return new FakeLikeRepository();
    }
  }

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

  @DisplayName("좋아요를 생성할 때,")
  @Nested
  class Create {
    @DisplayName("좋아요를 생성하면, 생성된 좋아요 정보를 반환한다.")
    @Test
    void 성공_좋아요_생성() {
      // arrange

      // act
      Like result = likeService.save(Like.create(savedUser, savedProduct));

      // assert
      assertThat(result).isNotNull();
      assertThat(result.getUser()).isNotNull();
    }

    @DisplayName("동일한 좋아요를 중복 생성하면, 멱등성이 보장된다.")
    @Test
    void 성공_중복_좋아요_멱등성() {
      // arrange
      Like result1 = likeService.save(Like.create(savedUser, savedProduct));

      // act
      Like result2 = likeService.save(Like.create(savedUser, savedProduct));

      // assert
      assertLike(result1, result2);
    }
  }

  @DisplayName("좋아요를 삭제할 때,")
  @Nested
  class Delete {
    @DisplayName("존재하는 좋아요를 삭제한다.")
    @Test
    void 성공_존재하는_좋아요ID() {
      // arrange
      Like result1 = likeService.save(Like.create(savedUser, savedProduct));

      // act
      likeService.remove(savedUser.getId(), savedProduct.getId());
      //assert
      boolean result = likeService.isLiked(savedUser.getId(), savedProduct.getId());
      assertThat(result).isFalse();
    }

    @DisplayName("존재하지 않는 좋아요 ID를 삭제하면, 예외가 발생하지 않는다.")
    @Test
    void 성공_이미_삭제된_좋아요() {
      // arrange
      Like result1 = likeService.save(Like.create(savedUser, savedProduct));

      // act
      likeService.remove(savedUser.getId(), savedProduct.getId());
      likeService.remove(savedUser.getId(), savedProduct.getId());

      // assert
      boolean result = likeService.isLiked(savedUser.getId(), savedProduct.getId());
      assertThat(result).isFalse();
    }
  }

  @DisplayName("좋아요 상품목록을 조회할 때,")
  @Nested
  class GetList {
    @DisplayName("페이징 처리되어, 초기설정시 size=20, sort=최신순으로 목록이 조회된다.")
    @Test
    void 성공_좋아요_상품목록조회() {
      // arrange
      Like savedLike = likeService.save(Like.create(savedUser, savedProduct));

      // act
      Page<Product> productsPage = likeService.getLikedProducts(savedUser.getId(), "latest", 0, 20);
      List<Product> products = productsPage.getContent();

      // assert
      assertThat(products).isNotEmpty().hasSize(1);
    }

  }
}
