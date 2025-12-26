package com.loopers.domain.product;

import com.loopers.domain.common.vo.Price;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("상품 서비스(ProductService) 테스트")
public class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @DisplayName("상품 ID로 상품을 조회할 때, ")
    @Nested
    class GetProductById {
        @DisplayName("존재하는 상품 ID로 조회하면 상품을 반환한다. (Happy Path)")
        @Test
        void should_returnProduct_when_productExists() {
            // arrange
            Long productId = 1L;
            Product product = Product.create("상품명", 1L, new Price(10000));
            setProductId(product, productId);
            when(productRepository.findById(productId)).thenReturn(Optional.of(product));

            // act
            Optional<Product> result = productService.getProductById(productId);

            // assert
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(1L);
            assertThat(result.get().getName()).isEqualTo("상품명");
        }

        @DisplayName("존재하지 않는 상품 ID로 조회하면 빈 Optional을 반환한다. (Exception)")
        @Test
        void should_returnEmptyOptional_when_productNotFound() {
            // arrange
            Long productId = 999L;
            when(productRepository.findById(productId)).thenReturn(Optional.empty());

            // act
            Optional<Product> result = productService.getProductById(productId);

            // assert
            assertThat(result).isEmpty();
        }
    }

    @DisplayName("여러 상품 ID로 상품 맵을 조회할 때, ")
    @Nested
    class GetProductMapByIds {
        @DisplayName("존재하는 상품 ID들로 조회하면 상품 맵을 반환한다. (Happy Path)")
        @Test
        void should_returnProductMap_when_productsExist() {
            // arrange
            List<Long> productIds = List.of(1L, 2L, 3L);
            List<Product> products = List.of(
                    setProductId(Product.create("상품1", 1L, new Price(10000)), 1L),
                    setProductId(Product.create("상품2", 1L, new Price(20000)), 2L),
                    setProductId(Product.create("상품3", 2L, new Price(15000)), 3L)
            );
            when(productRepository.findAllByIdIn(productIds)).thenReturn(products);

            // act
            Map<Long, Product> result = productService.getProductMapByIds(productIds);

            // assert
            assertThat(result).hasSize(3);
            assertThat(result.get(1L).getName()).isEqualTo("상품1");
            assertThat(result.get(2L).getName()).isEqualTo("상품2");
            assertThat(result.get(3L).getName()).isEqualTo("상품3");
        }

        @DisplayName("빈 ID 리스트로 조회하면 빈 맵을 반환한다. (Edge Case)")
        @Test
        void should_returnEmptyMap_when_emptyIdList() {
            // arrange
            List<Long> productIds = Collections.emptyList();
            when(productRepository.findAllByIdIn(productIds)).thenReturn(Collections.emptyList());

            // act
            Map<Long, Product> result = productService.getProductMapByIds(productIds);

            // assert
            assertThat(result).isEmpty();
        }

        @DisplayName("존재하지 않는 상품 ID들로 조회하면 빈 맵을 반환한다. (Edge Case)")
        @Test
        void should_returnEmptyMap_when_productsNotFound() {
            // arrange
            List<Long> productIds = List.of(999L, 1000L);
            when(productRepository.findAllByIdIn(productIds)).thenReturn(Collections.emptyList());

            // act
            Map<Long, Product> result = productService.getProductMapByIds(productIds);

            // assert
            assertThat(result).isEmpty();
        }
    }

    @DisplayName("상품 목록을 조회할 때, ")
    @Nested
    class GetProducts {
        @DisplayName("기본 페이지네이션으로 조회하면 상품 페이지를 반환한다. (Happy Path)")
        @Test
        void should_returnProductPage_when_defaultPageable() {
            // arrange
            Pageable pageable = PageRequest.of(0, 20);
            List<Product> products = List.of(
                    setProductId(Product.create("상품1", 1L, new Price(10000)), 1L),
                    setProductId(Product.create("상품2", 1L, new Price(20000)), 2L)
            );
            Page<Product> productPage = new PageImpl<>(products, pageable, 2);
            when(productRepository.findAll(pageable)).thenReturn(productPage);

            // act
            Page<Product> result = productService.getProducts(pageable);

            // assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
        }

        @DisplayName("최신순 정렬로 조회하면 상품 페이지를 반환한다. (Happy Path)")
        @Test
        void should_returnProductPage_when_sortedByLatest() {
            // arrange
            Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
            Pageable pageable = PageRequest.of(0, 20, sort);
            List<Product> products = List.of(
                    setProductId(Product.create("상품1", 1L, new Price(10000)), 1L),
                    setProductId(Product.create("상품2", 1L, new Price(20000)), 2L)
            );
            Page<Product> productPage = new PageImpl<>(products, pageable, 2);
            when(productRepository.findAll(pageable)).thenReturn(productPage);

            // act
            Page<Product> result = productService.getProducts(pageable);

            // assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getSort()).isEqualTo(sort);
        }

        @DisplayName("가격 오름차순 정렬로 조회하면 상품 페이지를 반환한다. (Happy Path)")
        @Test
        void should_returnProductPage_when_sortedByPriceAsc() {
            // arrange
            Sort sort = Sort.by(Sort.Direction.ASC, "price");
            Pageable pageable = PageRequest.of(0, 20, sort);
            List<Product> products = List.of(
                    setProductId(Product.create("상품1", 1L, new Price(10000)), 1L),
                    setProductId(Product.create("상품2", 1L, new Price(20000)), 2L)
            );
            Page<Product> productPage = new PageImpl<>(products, pageable, 2);
            when(productRepository.findAll(pageable)).thenReturn(productPage);

            // act
            Page<Product> result = productService.getProducts(pageable);

            // assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getSort()).isEqualTo(sort);
        }

        @DisplayName("좋아요 내림차순 정렬로 조회하면 상품 페이지를 반환한다. (Happy Path)")
        @Test
        void should_returnProductPage_when_sortedByLikesDesc() {
            // arrange
            Sort sort = Sort.by(Sort.Direction.DESC, "likes");
            Pageable pageable = PageRequest.of(0, 20, sort);
            List<Product> products = List.of(
                    setProductId(Product.create("상품1", 1L, new Price(10000)), 1L),
                    setProductId(Product.create("상품2", 1L, new Price(20000)), 2L)
            );
            Page<Product> productPage = new PageImpl<>(products, pageable, 2);
            when(productRepository.findAll(pageable)).thenReturn(productPage);

            // act
            Page<Product> result = productService.getProducts(pageable);

            // assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getSort()).isEqualTo(sort);
        }

        @DisplayName("빈 페이지로 조회하면 빈 상품 페이지를 반환한다. (Edge Case)")
        @Test
        void should_returnEmptyPage_when_noProducts() {
            // arrange
            Pageable pageable = PageRequest.of(0, 20);
            Page<Product> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
            when(productRepository.findAll(pageable)).thenReturn(emptyPage);

            // act
            Page<Product> result = productService.getProducts(pageable);

            // assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
        }
    }

    // Note: calculateTotalAmount 메서드가 ProductService에서 제거되었습니다.
    // 총 가격 계산은 OrderFacade에서 직접 처리하도록 변경되었습니다.

    /**
     * 리플렉션을 사용하여 Product의 id 필드 설정 (테스트 전용)
     */
    private Product setProductId(Product product, Long id) {
        try {
            Field idField = Product.class.getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(product, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set product id", e);
        }
        return product;
    }
}
