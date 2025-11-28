package com.loopers.domain.product;

import com.loopers.utils.ProductDataGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ProductPerformanceTest {

    @Autowired
    private ProductDataGenerator dataGenerator;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        dataGenerator.generate100KProducts();
    }

    @Test
    @DisplayName("10만 건 데이터에서 브랜드 필터링 및 가격 정렬 성능 테스트")
    void testPerformanceWith100KData() {
        long startTime = System.currentTimeMillis();

        ProductSearchCondition condition = new ProductSearchCondition(
                1L,
                ProductSortType.PRICE_ASC,
                PageRequest.of(0, 20)
        );

        Page<Product> result = productRepository.findProducts(
                condition.toPageRequest(),
                condition.brandId()
        );

        long duration = System.currentTimeMillis() - startTime;

        System.out.println("조회 시간: " + duration + "ms");
        System.out.println("전체 건수: " + result.getTotalElements());

        assertThat(duration).isLessThan(100);
        assertThat(result.getContent()).hasSize(20);
    }
}
