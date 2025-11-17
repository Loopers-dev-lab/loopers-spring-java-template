package com.loopers.domain.product;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService 단위 테스트")
class ProductServiceTest {

  @Mock
  private ProductRepository productRepository;

  @InjectMocks
  private ProductService productService;

  @Nested
  @DisplayName("상품 목록 조회")
  class FindProducts {

    @Test
    @DisplayName("brandId가 null이면 전체 상품 목록을 조회한다")
    void findProducts_withNullBrandId() {
      // given
      Pageable pageable = PageRequest.of(0, 10);
      Page<Product> expectedPage = new PageImpl<>(List.of());
      given(productRepository.findAll(pageable)).willReturn(expectedPage);

      // when
      Page<Product> result = productService.findProducts(null, pageable);

      // then
      assertThat(result).isEqualTo(expectedPage);
      then(productRepository).should(times(1)).findAll(pageable);
      then(productRepository).should(times(0)).findByBrandId(null, pageable);
    }

    @Test
    @DisplayName("brandId가 있으면 해당 브랜드의 상품 목록을 조회한다")
    void findProducts_withBrandId() {
      // given
      Long brandId = 1L;
      Pageable pageable = PageRequest.of(0, 10);
      Page<Product> expectedPage = new PageImpl<>(List.of());
      given(productRepository.findByBrandId(brandId, pageable)).willReturn(expectedPage);

      // when
      Page<Product> result = productService.findProducts(brandId, pageable);

      // then
      assertThat(result).isEqualTo(expectedPage);
      then(productRepository).should(times(1)).findByBrandId(brandId, pageable);
      then(productRepository).should(times(0)).findAll(pageable);
    }
  }
}
