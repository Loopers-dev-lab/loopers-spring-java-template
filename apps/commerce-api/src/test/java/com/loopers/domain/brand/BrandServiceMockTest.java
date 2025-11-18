package com.loopers.domain.brand;

import com.loopers.application.brand.BrandCommand;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class BrandServiceMockTest {
    @Mock
    BrandRepository brandRepository;

    @InjectMocks
    BrandService brandService;

    @Nested
    @DisplayName("BrandService create 테스트")
    class Create {
        private BrandModel brandModel;
        private BrandCommand.Create createCommand;

        @BeforeEach
        void setUp() {
            String brandName = "test";
            String description = "this is test brand";
            Character init_status = '0';
            brandModel = new BrandModel(brandName, description, BrandStatus.REGISTERED);
            createCommand = new BrandCommand.Create(brandName, description, BrandStatus.REGISTERED);
        }

        @Test
        @DisplayName("브랜드이름으로 중복 브랜드가 없다면, create하고 그때 BrandModel을 반환")
        public void returnBrandModel_whenValidBrandModel() {
            // given
            given(brandRepository.existsByName(brandModel.getName()))
                    .willReturn(false);

            given(brandRepository.save(any(BrandModel.class)))
                    .willAnswer(invocation -> {
                        BrandModel brand = invocation.getArgument(0);
                        // Mock 환경에서는 DB가 없으므로 @GeneratedValue가 동작하지 않는다
                        ReflectionTestUtils.setField(brand, "id", 1L);
                        return brand;
                    });

            // when
            BrandModel result = brandService.createBrandModel(createCommand);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo(createCommand.name());
            verify(brandRepository).existsByName(createCommand.name());
            verify(brandRepository).save(any(BrandModel.class));
        }

    }

    @Nested
    @DisplayName("BrandService update/delete 테스트")
    class Update {

        private BrandModel brandModel;
        private BrandCommand.Create createCommand;

        @BeforeEach
        void setUp() {
            String brandName = "test";
            String description = "this is test brand";
            Character init_status = 'Z';
            brandModel = new BrandModel(brandName, description,BrandStatus.DISCONITNUED);
            createCommand = new BrandCommand.Create(brandName, description, BrandStatus.DISCONITNUED);
        }

        @Test
        @DisplayName("브랜드이름으로 중복 브랜드가 있지만 등록상태가 아니였다면 상태를 등록상태로 변경한다")
        public void returnUpdatedBrandModel_whenValidBrandModel() {
            // given
            // BrandModel spyBrand = spy(brandModel);
            // doReturn(false).when(spyBrand).isRegistered();
            given(brandRepository.existsByName(anyString())).willReturn(true);
            given(brandRepository.findByName(anyString())).willReturn(Optional.of(brandModel));
            given(brandRepository.save(any(BrandModel.class))).willAnswer(invocation -> invocation.getArgument(0));


            // when
            BrandModel result = brandService.createBrandModel(createCommand);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo(createCommand.name());
            assertTrue(result.isRegistered());
            verify(brandRepository).save(any(BrandModel.class));

        }

        @Test
        @DisplayName("등록 상태인 브랜드는 해지할 수 있다")
        public void returnRows_whenRegisteredBrandModelDiscontinue() {
            // given
            BrandModel previous = new BrandModel("Nike", "Nike", BrandStatus.REGISTERED);
            given(brandRepository.findByName(previous.getName())).willReturn(Optional.of(previous));
            given(brandRepository.save(any(BrandModel.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            boolean result = brandService.discontinueBrand(previous.getName());

            //then
            assertThat(result).isTrue();
            assertThat(previous.isDiscontinued()).isTrue();
            then(brandRepository).should().save(previous);
        }

    }

    @Nested
    @DisplayName("BrandService read 테스트")
    class Read {
        private BrandModel brandModel;
        private BrandCommand.Create createCommand;

        @BeforeEach
        void setUp() {
            String brandName = "test";
            String description = "this is test brand";
            brandModel = new BrandModel(brandName, description, BrandStatus.REGISTERED);
        }

        @Test
        @DisplayName("등록되지 않은 브랜드 정보 조회를 하면 NOT_FOUND 에러가 반환된다")
        public void returnBadRequest_whenNotFoundBrandModel() {
            given(brandRepository.findByName(anyString())).willReturn(Optional.empty());

            CoreException result = assertThrows(CoreException.class, () -> {
                brandService.getBrandByName(anyString());
            });

            // then
            AssertionsForClassTypes.assertThat(result.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        }


        @Test
        @DisplayName("브랜드 목록을 조회하면 현재 등록된 브랜드 목록이 조회된다")
        public void returnRegisteredBrandList_whenBrandListIsExist() {
            // given
            List<BrandModel> brandList = List.of(
                    new BrandModel("Nike", "desc", BrandStatus.REGISTERED),
                    new BrandModel("Adidas", "desc", BrandStatus.REGISTERED),
                    new BrandModel("Puma", "desc", BrandStatus.REGISTERED)
            );
            given(brandRepository.findAllByStatus(BrandStatus.REGISTERED)).willReturn(brandList);

            // when
            List<BrandModel> result = brandService.getBrands();

            // then
            assertThat(result).isNotNull();
            assertThat(result.size()).isEqualTo(brandList.size());
        }
    }
}
