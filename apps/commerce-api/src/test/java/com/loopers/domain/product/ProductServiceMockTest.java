package com.loopers.domain.product;

import com.loopers.domain.brand.BrandModel;
import com.loopers.domain.brand.BrandStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
public class ProductServiceMockTest {
    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private String name;
    private String category;
    private Integer price;
    private Integer stock;
    private ProductStatus status;
    private BrandModel brand;
    private ProductModel product;


    @BeforeEach
    void setUp() {
        name = "test_product";
        category = "test_category";
        price = 100;
        stock = 100;
        status = ProductStatus.ON_SALE;
        brand = new BrandModel("TEST", "TEST", BrandStatus.REGISTERED);
        product = new ProductModel(name, category, price, stock, status, brand);
    }

    @Test
    @DisplayName("정렬조건이 없으면 default 정렬조건(최근등록순, createdAt DESC, id DESC)으로 조회한다")
    void getProducts_defaultSort() {
        //given
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProductModel> emptyPage = Page.empty();
        given(productRepository.findByStatusNot(any(), any())).willReturn(emptyPage);

        // when
        productService.getProductsNotStopSelling(null, pageable);

        // then
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        then(productRepository).should()
                .findByStatusNot(eq(ProductStatus.STOP_SELLING), pageableCaptor.capture());

        Pageable usedPageable = pageableCaptor.getValue();
        Sort sort = usedPageable.getSort();

        // createdAt desc, id desc 인지만 검증
        assertThat(sort.getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(sort.getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @DisplayName("정렬조건이 가격 오름차순 정렬조건이면 (price ASC, id ASC)으로 조회한다")
    void getProducts_priceAscSort() {
        //given
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProductModel> emptyPage = Page.empty();
        given(productRepository.findByStatusNot(any(), any())).willReturn(emptyPage);

        // when
        productService.getProductsNotStopSelling(ProductSortType.PRICE_ASC, pageable);

        // then
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        then(productRepository).should()
                .findByStatusNot(eq(ProductStatus.STOP_SELLING), pageableCaptor.capture());

        Pageable usedPageable = pageableCaptor.getValue();
        Sort sort = usedPageable.getSort();

        // createdAt desc, id desc 인지만 검증
        assertThat(sort.getOrderFor("price").getDirection()).isEqualTo(Sort.Direction.ASC);
        assertThat(sort.getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    @DisplayName("상품이 존재하면 상품 상세 정보를 리턴한다")
    void getProductDetail_success() {
        // given
        Long productId = 10L;
        given(productRepository.findByIdForUpdate(productId)).willReturn(Optional.of(product));

        // when
        ProductModel result = productService.getProductDetail(productId);

        // then
        assertThat(result).isEqualTo(product);
    }


}
