package com.loopers.domain.stock;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandFixture;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductFixture;
import com.loopers.domain.product.ProductRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
@Transactional
class StockServiceIntegrationTest {
  @Autowired
  private StockService sut;

  @MockitoSpyBean
  private BrandRepository brandRepository;
  @MockitoSpyBean
  private ProductRepository productRepository;
  @MockitoSpyBean
  private StockRepository stockRepository;
  @Autowired
  private DatabaseCleanUp databaseCleanUp;

  List<Brand> savedBrands;
  List<Product> savedProducts;
  Stock savedStock;

  @BeforeEach
  void setup() {
    List<Brand> brandList = List.of(BrandFixture.createBrand(), BrandFixture.createBrand());
    savedBrands = brandRepository.saveAll(brandList);

    List<Product> productList = List.of(ProductFixture.createProduct(savedBrands.get(0))
        , ProductFixture.createProduct(savedBrands.get(0))
        , ProductFixture.createProduct(savedBrands.get(1)));
    savedProducts = productRepository.saveAll(productList);

    Stock stock = StockFixture.createStockWith(savedProducts.get(0).getId(), 10);
    savedStock = stockRepository.save(stock);
  }

  @AfterEach
  void tearDown() {
    databaseCleanUp.truncateAllTables();
  }

  @DisplayName("재고를 조회할 때,")
  @Nested
  class Get {
    @DisplayName("존재하는 상품 ID를 주면, 해당 상품의 재고 정보를 반환한다.")
    @Test
    void 성공_존재하는_상품ID() {
      // arrange
      Long productId = savedProducts.get(0).getId();
      // act
      Stock result = sut.getStock(productId);

      // assert
      assertThat(result.getAvailable()).isEqualTo(savedStock.getAvailable());
    }

    @DisplayName("존재하지 않는 상품 ID를 주면, 재고0이 반환된다.")
    @Test
    void 실패_존재하지_않는_상품ID() {
      // arrange
      Long productId = (long) -1;
      // act
      Stock result = sut.getStock(productId);

      // assert
      assertThat(result).isNotNull();
      assertThat(result.getAvailable()).isZero();
    }
  }

}
