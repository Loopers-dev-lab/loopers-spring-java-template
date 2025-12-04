package com.loopers.application.like;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandFixture;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.like.LikeService;
import com.loopers.domain.order.Money;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductFixture;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserFixture;
import com.loopers.domain.user.UserRepository;
import com.loopers.domain.user.UserService;
import com.loopers.utils.DatabaseCleanUp;
import com.loopers.utils.RedisCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
class LikeFacadeIntegrationTest {
  @Autowired
  private LikeFacade sut;
  @MockitoSpyBean
  private UserRepository userRepository;
  @MockitoSpyBean
  private UserService userService;
  @MockitoSpyBean
  private PointService pointService;
  @MockitoSpyBean
  private BrandService brandService;
  @MockitoSpyBean
  private ProductService productService;
  @MockitoSpyBean
  private LikeService likeService;

  @MockitoSpyBean
  private OrderService orderService;
  @Autowired
  private RedisCleanUp redisCleanUp;

  @Autowired
  private DatabaseCleanUp databaseCleanUp;
  List<User> savedUsers;
  List<Brand> savedBrands;
  List<Product> savedProducts;

  @BeforeEach
  void setup() {
    // arrange
    List<User> userList = List.of(UserFixture.createUserWithLoginId("user1"), UserFixture.createUserWithLoginId("user2"));
    savedUsers = List.of(userService.join(userList.get(0)), userService.join(userList.get(1)));

    List<Brand> brandList = List.of(BrandFixture.createBrand(), BrandFixture.createBrand());
    savedBrands = brandService.saveAll(brandList);

    List<Product> productList = List.of(ProductFixture.createProductWith("product1", Money.wons(1))
        , ProductFixture.createProductWith("product2", Money.wons(4))
        , ProductFixture.createProduct(savedBrands.get(1)));
    savedProducts = productService.saveAll(productList);
    redisCleanUp.truncateAll();
  }

  @AfterEach
  void tearDown() {
    databaseCleanUp.truncateAllTables();
  }

  @DisplayName("좋아요를 할 때,")
  @Nested
  class Post {
    @DisplayName("좋아요가 성공한다.")
    @Test
    void 성공_좋아요() {

      Long userId = savedUsers.get(0).getId();
      Long productId = savedProducts.get(0).getId();

      // act
      LikeInfo savedLike = sut.like(userId, productId);
      // assert
      assertThat(savedLike).isNotNull();
      assertThat(savedLike.isLiked()).isTrue();
      assertThat(savedLike.likeCount()).isEqualTo(1);
    }

    @DisplayName("좋아요가 성공한다.")
    @Test
    void 성공_좋아요_중복() {
      Long userId = savedUsers.get(0).getId();
      Long productId = savedProducts.get(0).getId();
      sut.like(userId, productId);
      // act
      LikeInfo savedLike = sut.like(userId, productId);
      // assert
      assertThat(savedLike).isNotNull();
      assertThat(savedLike.isLiked()).isTrue();
      assertThat(savedLike.likeCount()).isEqualTo(1);
    }
  }

  @DisplayName("좋아요 취소할 때,")
  @Nested
  class Delete {
    @DisplayName("좋아요 취소가 성공한다.")
    @Test
    void 성공_좋아요_취소() {
      Long userId = savedUsers.get(0).getId();
      Long productId = savedProducts.get(0).getId();

      // act
      LikeInfo savedLike = sut.unlike(userId, productId);
      // assert
      assertThat(savedLike).isNotNull();
      assertThat(savedLike.isLiked()).isFalse();
      assertThat(savedLike.likeCount()).isZero();
    }

    @DisplayName("좋아요 취소가 성공한다.")
    @Test
    void 성공_좋아요_취소_중복() {
      Long userId = savedUsers.get(0).getId();
      Long productId = savedProducts.get(0).getId();
      sut.unlike(userId, productId);
      // act
      LikeInfo savedLike = sut.unlike(userId, productId);
      // assert
      assertThat(savedLike).isNotNull();
      assertThat(savedLike.isLiked()).isFalse();
      assertThat(savedLike.likeCount()).isZero();
    }
  }
}
