package com.loopers.application.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandFixture;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.like.LikeRepository;
import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointRepository;
import com.loopers.domain.product.*;
import com.loopers.domain.stock.Stock;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserFixture;
import com.loopers.domain.user.UserRepository;
import com.loopers.domain.view.ProductListView;
import com.loopers.domain.view.ProductListViewRepository;
import com.loopers.support.error.CoreException;
import com.loopers.utils.DatabaseCleanUp;
import com.loopers.utils.RedisCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


@SpringBootTest
@Transactional
class ProductFacadeIntegrationTest {
  @Autowired
  private ProductFacade sut;
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private PointRepository pointRepository;
  @Autowired
  private BrandRepository brandRepository;
  @Autowired
  private ProductRepository productRepository;
  @Autowired
  private ProductListViewRepository productListViewRepository;

  @Autowired
  private LikeRepository likeRepository;
  @Autowired
  private DatabaseCleanUp databaseCleanUp;
  @Autowired
  private RedisCleanUp redisCleanUp;
  User savedUser;
  List<Product> savedProducts;

  @BeforeEach
  void setup() {
    // arrange
    savedUser = userRepository.save(UserFixture.createUser());
    pointRepository.save(Point.create(savedUser, BigDecimal.TEN));
    List<Brand> brandList = List.of(BrandFixture.createBrand(), BrandFixture.createBrand());
    List<Brand> savedBrands = brandRepository.saveAll(brandList);

    List<Product> productList = List.of(ProductFixture.createProduct(savedBrands.get(0))
        , ProductFixture.createProduct(savedBrands.get(0))
        , ProductFixture.createProduct(savedBrands.get(1)));
    savedProducts = productRepository.saveAll(productList);
    List<ProductListView> productListView = List.of(
        ProductListView.create(savedProducts.get(0), Stock.create(savedProducts.get(0).getId(), 0)),
        ProductListView.create(savedProducts.get(1), Stock.create(savedProducts.get(1).getId(), 0)),
        ProductListView.create(savedProducts.get(2), Stock.create(savedProducts.get(2).getId(), 0)));
    productListViewRepository.saveAll(productListView);
    redisCleanUp.truncateAll();
  }

  @AfterEach
  void tearDown() {
    databaseCleanUp.truncateAllTables();
  }

  @DisplayName("상품목록을 조회할 때,")
  @Nested
  class GetList {
    @DisplayName("페이징 처리되어, 초기설정시 size=20, sort=최신순으로 목록이 조회된다.")
    @Test
    void 성공_상품목록조회() {
      // arrange
      Long userId = savedUser.getId();
      Long brandId = null;
      likeRepository.save(savedUser.getId(), savedProducts.get(0).getId());
      // act
      Page<ProductWithLikeCount> productsPage = sut.getProductList(userId, brandId, "latest", 0, 20);
      List<ProductWithLikeCount> products = productsPage.getContent();
      // assert
      assertThat(products).isNotEmpty().hasSize(3);
      assertThat(products.get(2).name()).isEqualTo(savedProducts.get(0).getName());
      assertThat(products.get(2).likeCount()).isEqualTo(1);
    }

    @DisplayName("브랜드ID 검색조건 포함시, 해당 브랜드의 상품 목록이 조회된다.")
    @Test
    void 성공_상품목록조회_브랜드ID() {
      // arrange
      Long userId = savedUser.getId();
      Long brandId = savedProducts.get(0).getBrand().getId();
      // act
      Page<ProductWithLikeCount> productsPage = sut.getProductList(userId, brandId, null, 0, 20);
      List<ProductWithLikeCount> resultList = productsPage.getContent();

      // assert
      assertThat(resultList).isNotEmpty().hasSize(2);
      assertThat(resultList.get(0).name()).isEqualTo(savedProducts.get(1).getName());
      assertThat(resultList.get(1).name()).isEqualTo(savedProducts.get(0).getName());
    }
  }

  @DisplayName("상품을 조회할 때,")
  @Nested
  class Get {
    @DisplayName("존재하는 상품 ID를 주면, 해당 상품 정보를 반환한다.")
    @Test
    void 성공_존재하는_상품ID() {
      // arrange
      Long productId = savedProducts.get(0).getId();
      // act
      ProductDetailInfo result = sut.getProductDetail(savedUser.getId(), productId);
      // assert
      assertThat(result.name()).isEqualTo(savedProducts.get(0).getName());
    }

    @DisplayName("존재하지 않는 상품 ID를 주면, 예외가 반환된다.")
    @Test
    void 실패_존재하지_않는_상품ID() {
      // arrange
      Long productId = (long) -1;
      // act
      // assert
      assertThrows(CoreException.class, () -> {
        sut.getProductDetail(savedUser.getId(), productId);
      });
    }
  }

}
