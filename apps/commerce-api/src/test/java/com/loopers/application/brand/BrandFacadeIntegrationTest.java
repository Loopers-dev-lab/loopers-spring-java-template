package com.loopers.application.brand;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
class BrandFacadeIntegrationTest {

    @Autowired
    private BrandFacade brandFacade;

    @MockitoSpyBean
    private BrandService brandService;

    @DisplayName("브랜드 정보를 조회할 때,")
    @Nested
    class GetBrand {

        @DisplayName("브랜드가 존재하면 브랜드 정보가 반환된다.")
        @Test
        void returnsBrandInfo_whenBrandExists() {
            // arrange
            Long brandId = 1L;
            Brand brand = createBrand(brandId, "브랜드명", "브랜드 설명");

            doReturn(Optional.of(brand)).when(brandService).findBrandById(brandId);

            // act
            BrandInfo result = brandFacade.getBrand(brandId);

            // assert
            assertAll(
                    () -> assertThat(result.id()).isEqualTo(1L),
                    () -> assertThat(result.name()).isEqualTo("브랜드명"),
                    () -> assertThat(result.description()).isEqualTo("브랜드 설명")
            );

            // verify
            verify(brandService, times(1)).findBrandById(brandId);
        }

        @DisplayName("브랜드가 존재하지 않으면 NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsException_whenBrandDoesNotExist() {
            // arrange
            Long brandId = 999L;

            doReturn(Optional.empty()).when(brandService).findBrandById(brandId);

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                brandFacade.getBrand(brandId);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
            assertThat(exception.getMessage()).contains("브랜드를 찾을 수 없습니다.");

            // verify
            verify(brandService, times(1)).findBrandById(brandId);
        }
    }

    private Brand createBrand(Long id, String name, String description) {
        Brand brand = mock(Brand.class);
        when(brand.getId()).thenReturn(id);
        when(brand.getName()).thenReturn(name);
        when(brand.getDescription()).thenReturn(description);
        return brand;
    }
}

