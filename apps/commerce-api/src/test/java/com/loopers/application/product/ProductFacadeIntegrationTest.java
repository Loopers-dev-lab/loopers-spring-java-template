package com.loopers.application.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandFixture;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductFixture;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserFixture;
import com.loopers.domain.user.UserRepository;
import com.loopers.support.error.CoreException;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;

import java.util.List;

import static com.loopers.domain.product.ProductAssertions.assertProduct;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


@SpringBootTest
class ProductFacadeIntegrationTest {
  @Autowired
  private ProductFacade sut;
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private BrandRepository brandRepository;
  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private DatabaseCleanUp databaseCleanUp;

  User savedUser;
  List<Product> savedProducts;

  @BeforeEach
  void setup() {
    // arrange
    savedUser = userRepository.save(UserFixture.createUser());
    List<Brand> brandList = List.of(BrandFixture.createBrand(), BrandFixture.createBrand());
    List<Brand> savedBrands = brandRepository.saveAll(brandList);

    List<Product> productList = List.of(ProductFixture.createProduct(savedBrands.get(0))
        , ProductFixture.createProduct(savedBrands.get(0))
        , ProductFixture.createProduct(savedBrands.get(1)));
    savedProducts = productRepository.saveAll(productList);
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
      Long brandId = null;
      // act
      Page<Product> productsPage = sut.getProductList(brandId, "latest", 0, 20);
      List<Product> products = productsPage.getContent();
      // assert
      assertThat(products).isNotEmpty().hasSize(3);
    }

    @DisplayName("브랜드ID 검색조건 포함시, 해당 브랜드의 상품 목록이 조회된다.")
    @Test
    void 성공_상품목록조회_브랜드ID() {
      // arrange
      Long brandId = savedProducts.get(0).getBrand().getId();
      // act
      Page<Product> productsPage = sut.getProductList(brandId, null, 0, 20);
      List<Product> resultList = productsPage.getContent();

      // assert
      assertThat(resultList).isNotEmpty().hasSize(2);
      assertProduct(resultList.get(0), savedProducts.get(1));
      assertProduct(resultList.get(1), savedProducts.get(0));
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
