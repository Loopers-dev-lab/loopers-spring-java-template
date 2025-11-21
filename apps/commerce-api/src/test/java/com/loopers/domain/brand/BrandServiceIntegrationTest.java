package com.loopers.domain.brand;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class BrandServiceIntegrationTest {

    @Autowired
    private BrandService brandService;

    @MockitoSpyBean
    private BrandRepository brandRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("ID로 브랜드 조회를 할 수 있다.")
    @Test
    void getBrand() {
        // arrange
        Brand savedBrand = brandRepository.save(Brand.create("Test Brand"));

        // act
        Brand foundBrand = brandService.getBrand(savedBrand.getId());

        // assert
        assertAll(
                () -> assertThat(foundBrand).isNotNull(),
                () -> assertThat(foundBrand.getId()).isEqualTo(savedBrand.getId()),
                () -> assertThat(foundBrand.getName()).isEqualTo("Test Brand")
        );
    }

    @DisplayName("존재하지 않는 ID로 브랜드 조회 시, NOT_FOUND 예외가 발생한다.")
    @Test
    void getBrand_throwsNotFound() {
        // act & assert
        CoreException exception = assertThrows(CoreException.class, () -> {
            brandService.getBrand(-1L);
        });

        assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
    }
}
