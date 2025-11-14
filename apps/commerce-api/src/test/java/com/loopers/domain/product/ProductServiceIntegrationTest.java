package com.loopers.domain.product;

import com.loopers.domain.brand.Brand;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static com.loopers.domain.brand.BrandAssertions.assertBrand;
import static com.loopers.domain.product.ProductAssertions.assertProduct;
import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
@Transactional
class ProductServiceIntegrationTest {
  @Autowired
  private ProductService productService;

  @MockitoSpyBean
  private BrandJpaRepository brandJpaRepository;
  @MockitoSpyBean
  private ProductJpaRepository productJpaRepository;

  @Autowired
  private DatabaseCleanUp databaseCleanUp;

  List<Brand> savedBrands;
  List<Product> savedProducts;

  @BeforeEach
  void setup() {
    List<Brand> brandList = List.of(Brand.create("레이브", "레이브는 음악, 영화, 예술 등 다양한 문화에서 영감을 받아 경계 없고 자유분방한 스타일을 제안하는 패션 레이블입니다.")
        , Brand.create("마뗑킴", "마뗑킴은 트렌디하면서도 편안함을 더한 디자인을 선보입니다. 일상에서 조화롭게 적용할 수 있는 자연스러운 패션 문화를 지향합니다."));
    savedBrands = brandList.stream().map((brand) -> brandJpaRepository.save(brand)).toList();

    List<Product> productList = List.of(Product.create(savedBrands.get(0), "Wild Faith Rose Sweatshirt", new BigDecimal(80_000), 10)
        , Product.create(savedBrands.get(0), "Flower Pattern Fleece Jacket", new BigDecimal(178_000), 20)
        , Product.create(savedBrands.get(1), "Flower Pattern Fleece Jacket", new BigDecimal(178_000), 20)
    );
    savedProducts = productList.stream().map((product) -> productJpaRepository.save(product)).toList();

  }

  @AfterEach
  void tearDown() {
    databaseCleanUp.truncateAllTables();
  }

  @DisplayName("상품목록을 조회할 때,")
  @Nested
  class GetList {
    @DisplayName("페이징 처리되어, 초기설정시 size=0, sort=최신순으로 목록이 조회된다.")
    @Test
    void 성공_상품목록조회() {
      // arrange

      // act
      Page<Product> productsPage = productService.getProducts(null, "latest", 0, 20);
      List<Product> products = productsPage.getContent();
      // assert
      assertThat(products).isNotEmpty().hasSize(3);
    }

    @DisplayName("브랜드ID 검색조건 포함시, 해당 브랜드의 상품 목록이 조회된다.")
    @Test
    void 성공_상품목록조회_브랜드ID() {
      // arrange

      // act
      Page<Product> productsPage = productService.getProducts(savedProducts.get(0).getId(), null, 0, 20);
      List<Product> productList = productsPage.getContent();

      // assert
      assertThat(productList).isNotEmpty().hasSize(2);

      assertProduct(productList.get(0), savedProducts.get(1));
      assertProduct(productList.get(1), savedProducts.get(0));
    }
  }

  @DisplayName("상품을 조회할 때,")
  @Nested
  class Get {
    @DisplayName("존재하는 상품 ID를 주면, 해당 상품 정보를 반환한다.")
    @Test
    void 성공_존재하는_상품ID() {
      // arrange
      // act
      Product result = productService.getProduct(savedProducts.get(0).getId());

      // assert
      assertProduct(result, savedProducts.get(0));
      assertBrand(result.getBrand(), savedBrands.get(0));
    }

    @DisplayName("존재하지 않는 상품 ID를 주면, null이 반환된다.")
    @Test
    void 실패_존재하지_않는_상품ID() {
      // arrange

      // act
      Product result = productService.getProduct((long) 1000);

      // assert
      assertThat(result).isNull();
    }
  }

}
