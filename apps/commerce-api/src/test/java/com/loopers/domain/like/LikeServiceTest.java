package com.loopers.domain.like;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Product;
import com.loopers.domain.user.UserModel;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.like.LikeJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.util.List;

import static com.loopers.domain.like.LikeAssertions.assertLike;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


@SpringBootTest
class LikeServiceTest {
  @Autowired
  private LikeService likeService;

  @TestConfiguration
  static class FakeRepositoryConfig {

    @Primary
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

  UserModel savedUser;
  Brand savedBrand;
  Product savedProduct;

  @BeforeEach
  void setup() {
    UserModel user = UserModel.create("user1", "user1@test.XXX", "1999-01-01", "F");
    savedUser = userJpaRepository.save(user);
    Brand brand = Brand.create("레이브", "레이브는 음악, 영화, 예술 등 다양한 문화에서 영감을 받아 경계 없고 자유분방한 스타일을 제안하는 패션 레이블입니다.");
    savedBrand = brandJpaRepository.save(brand);
    Product product = Product.create(savedBrand, "Wild Faith Rose Sweatshirt", new BigDecimal(80_000), 10);
    savedProduct = productJpaRepository.save(product);

  }

  @AfterEach
  void tearDown() {
    databaseCleanUp.truncateAllTables();
  }

  @DisplayName("좋아요를 조회할 때,")
  @Nested
  class Create {
    @DisplayName("존재하는 좋아요 ID를 주면, 해당 좋아요 정보를 반환한다.")
    @Test
    void 성공_존재하는_좋아요ID() {
      // arrange

      // act
      Like result = likeService.save(Like.create(savedUser, savedProduct));

      // assert
      assertLike(result, Like.create(savedUser, savedProduct));
    }

    @DisplayName("존재하지 않는 좋아요 ID를 주면, 예외가 발생하지 않는다.")
    @Test
    void 성공_이미_존재하는_좋아요ID() {
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
    }
  }

  @DisplayName("좋아요 상품목록을 조회할 때,")
  @Nested
  class GetList {
    @DisplayName("페이징 처리되어, 초기설정시 size=0, sort=최신순으로 목록이 조회된다.")
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
