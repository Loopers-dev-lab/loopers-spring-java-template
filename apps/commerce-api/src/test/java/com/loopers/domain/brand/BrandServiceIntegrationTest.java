package com.loopers.domain.brand;

import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest
class BrandServiceIntegrationTest {
    @Autowired
    private BrandService brandService;

    @Autowired
    private BrandJpaRepository brandJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("브랜드를 조회할 때,")
    @Nested
    class Get {
        @DisplayName("존재하는 브랜드ID인 경우 정상 조회된다.")
        @Test
        void succeeds_whenValidIdIsProvided() {
            // arrange
            Brand brand = brandJpaRepository.save(
                    Brand.create("나이키", "운동복 브랜드입니다.")
            );

            // act
            Brand result = brandService.getBrand(brand.getId());

            // assert
            assertAll(
                () -> assertThat(result).isNotNull(),
                () -> assertThat(result.getId()).isEqualTo(brand.getId()),
                () -> assertThat(result.getName()).isEqualTo(brand.getName()),
                () -> assertThat(result.getDescription()).isEqualTo(brand.getDescription())
            );
        }

        @DisplayName("존재하지 않는 브랜드ID인 경우 조회 실패한다.")
        @Test
        void fails_whenInvalidIdIsProvided() {
            // arrange
            Long invalidId = 999L;

            // act + assert
            assertThatThrownBy(() -> brandService.getBrand(invalidId))
                    .hasMessageContaining("존재하지 않는 브랜드입니다.");
        }
    }
}
