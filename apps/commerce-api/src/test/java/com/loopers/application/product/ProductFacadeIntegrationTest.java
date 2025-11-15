package com.loopers.application.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.order.Money;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.user.User;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.support.error.CoreException;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.List;

import static com.loopers.domain.product.ProductAssertions.assertProduct;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


@SpringBootTest
class ProductFacadeIntegrationTest {
  @Autowired
  private ProductFacade productFacade;
  @MockitoSpyBean
  private UserJpaRepository userJpaRepository;
  @MockitoSpyBean
  private BrandJpaRepository brandJpaRepository;
  @MockitoSpyBean
  private ProductService productService;

  @Autowired
  private DatabaseCleanUp databaseCleanUp;

  User savedUser;
  List<Product> savedProducts;

  @BeforeEach
  void setup() {
    // arrange
    User user = User.create("user1", "user1@test.XXX", "1999-01-01", "F");
    savedUser = userJpaRepository.save(user);
    List<Brand> brandList = List.of(Brand.create("레이브", "레이브는 음악, 영화, 예술 등 다양한 문화에서 영감을 받아 경계 없고 자유분방한 스타일을 제안하는 패션 레이블입니다.")
        , Brand.create("마뗑킴", "마뗑킴은 트렌디하면서도 편안함을 더한 디자인을 선보입니다. 일상에서 조화롭게 적용할 수 있는 자연스러운 패션 문화를 지향합니다."));
    List<Brand> savedBrandList = brandList.stream().map((brand) -> brandJpaRepository.save(brand)).toList();

    List<Product> productList = List.of(Product.create(savedBrandList.get(0), "Wild Faith Rose Sweatshirt", Money.wons(80_000), 10)
        , Product.create(savedBrandList.get(0), "Flower Pattern Fleece Jacket", Money.wons(178_000), 20)
        , Product.create(savedBrandList.get(1), "Flower Pattern Fleece Jacket", Money.wons(178_000), 20)
    );
    savedProducts = productService.save(productList);

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
      // act
      Page<Product> productsPage = productFacade.getProductList(null, "latest", 0, 20);
      List<Product> products = productsPage.getContent();
      // assert
      assertThat(products).isNotEmpty().hasSize(3);
    }

    @DisplayName("브랜드ID 검색조건 포함시, 해당 브랜드의 상품 목록이 조회된다.")
    @Test
    void 성공_상품목록조회_브랜드ID() {
      // arrange
      // act
      Page<Product> productsPage = productFacade.getProductList(savedProducts.get(0).getId(), null, 0, 20);
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
      ProductDetailInfo result = productFacade.getProductDetail(savedUser.getId(), savedProducts.get(0).getId());

      // assert
      assertThat(result.name()).isEqualTo(savedProducts.get(0).getName());
    }

    @DisplayName("존재하지 않는 상품 ID를 주면, 예외가 반환된다.")
    @Test
    void 실패_존재하지_않는_상품ID() {
      // arrange
      // act
      // assert
      assertThrows(CoreException.class, () -> {
        productFacade.getProductDetail(savedUser.getId(), (long) 100);
      });
    }
  }

}
