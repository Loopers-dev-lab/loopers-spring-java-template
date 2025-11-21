package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Product 테스트")
public class ProductTest {

    @DisplayName("Product 엔티티 생성")
    @Nested
    class CreateProductTest {

        final String validName = "테스트 상품";
        final String validDescription = "테스트 설명";
        final BigDecimal validPrice = BigDecimal.valueOf(10000L);
        final ProductStatus validStatus = ProductStatus.ON_SALE;
        final Boolean validIsVisible = true;
        final Boolean validIsSellable = true;

        @DisplayName("성공 케이스: 필드가 모두 유효하면 Product 객체 생성 성공")
        @Test
        void createProduct_withValidFields_Success() {
            // arrange & act
            Product product = Product.builder()
                    .name(validName)
                    .description(validDescription)
                    .price(validPrice)
                    .status(validStatus)
                    .isVisible(validIsVisible)
                    .isSellable(validIsSellable)
                    .build();

            // assert
            assertNotNull(product);
            assertAll(
                    () -> assertEquals(product.getName(), validName),
                    () -> assertEquals(product.getDescription(), validDescription),
                    () -> assertEquals(product.getPrice(), validPrice),
                    () -> assertEquals(0L, product.getLikeCount(), "likeCount는 필드 초기화로 0L이 기본값"),
                    () -> assertEquals(product.getStatus(), validStatus),
                    () -> assertEquals(product.getIsVisible(), validIsVisible),
                    () -> assertEquals(product.getIsSellable(), validIsSellable)
            );
        }

        @DisplayName("Product name 유효성 검사")
        @Nested
        class NameTest {

            @DisplayName("실패 케이스: name이 null이면 Product 객체 생성 실패")
            @Test
            void createProduct_withNullName_BadRequest() {
                // arrange & act
                CoreException result = assertThrows(CoreException.class,
                        () -> Product.builder()
                                .name(null)
                                .description(validDescription)
                                .price(validPrice)
                                .status(validStatus)
                                .isVisible(validIsVisible)
                                .isSellable(validIsSellable)
                                .build()
                );

                // assert
                assertEquals(ErrorType.BAD_REQUEST, result.getErrorType());
                assertEquals("Product : name이 비어있을 수 없습니다.", result.getCustomMessage());
            }

            @DisplayName("실패 케이스: name이 빈 문자열이면 Product 객체 생성 실패")
            @Test
            void createProduct_withBlankName_BadRequest() {
                // arrange & act
                CoreException result = assertThrows(CoreException.class,
                        () -> Product.builder()
                                .name("")
                                .description(validDescription)
                                .price(validPrice)
                                .status(validStatus)
                                .isVisible(validIsVisible)
                                .isSellable(validIsSellable)
                                .build()
                );

                // assert
                assertEquals(ErrorType.BAD_REQUEST, result.getErrorType());
                assertEquals("Product : name이 비어있을 수 없습니다.", result.getCustomMessage());
            }

            @DisplayName("실패 케이스: name이 공백만 있으면 Product 객체 생성 실패")
            @Test
            void createProduct_withWhitespaceName_BadRequest() {
                // arrange & act
                CoreException result = assertThrows(CoreException.class,
                        () -> Product.builder()
                                .name("   ")
                                .description(validDescription)
                                .price(validPrice)
                                .status(validStatus)
                                .isVisible(validIsVisible)
                                .isSellable(validIsSellable)
                                .build()
                );

                // assert
                assertEquals(ErrorType.BAD_REQUEST, result.getErrorType());
                assertEquals("Product : name이 비어있을 수 없습니다.", result.getCustomMessage());
            }
        }

        @DisplayName("Product price 유효성 검사")
        @Nested
        class PriceTest {

            @DisplayName("실패 케이스: price가 null이면 Product 객체 생성 실패")
            @Test
            void createProduct_withNullPrice_BadRequest() {
                // arrange & act
                CoreException result = assertThrows(CoreException.class,
                        () -> Product.builder()
                                .name(validName)
                                .description(validDescription)
                                .price(null)
                                .status(validStatus)
                                .isVisible(validIsVisible)
                                .isSellable(validIsSellable)
                                .build()
                );

                // assert
                assertEquals(ErrorType.BAD_REQUEST, result.getErrorType());
                assertEquals("Product : price 가 비어있을 수 없습니다.", result.getCustomMessage());
            }

            @DisplayName("실패 케이스: price가 음수이면 Product 객체 생성 실패")
            @Test
            void createProduct_withNegativePrice_BadRequest() {
                // arrange & act
                CoreException result = assertThrows(CoreException.class,
                        () -> Product.builder()
                                .name(validName)
                                .description(validDescription)
                                .price(BigDecimal.valueOf(-1000L))
                                .status(validStatus)
                                .isVisible(validIsVisible)
                                .isSellable(validIsSellable)
                                .build()
                );

                // assert
                assertEquals(ErrorType.BAD_REQUEST, result.getErrorType());
                assertEquals("Product : price 는 음수가 될 수 없습니다.", result.getCustomMessage());
            }

            @DisplayName("성공 케이스: price가 0이면 Product 객체 생성 성공")
            @Test
            void createProduct_withZeroPrice_Success() {
                // arrange & act
                Product product = Product.builder()
                        .name(validName)
                        .description(validDescription)
                        .price(BigDecimal.ZERO)
                        .status(validStatus)
                        .isVisible(validIsVisible)
                        .isSellable(validIsSellable)
                        .build();

                // assert
                assertNotNull(product);
                assertEquals(BigDecimal.ZERO, product.getPrice());
            }
        }


        @DisplayName("Product status 유효성 검사")
        @Nested
        class StatusTest {

            @DisplayName("실패 케이스: status가 null이면 Product 객체 생성 실패")
            @Test
            void createProduct_withNullStatus_BadRequest() {
                // arrange & act
                CoreException result = assertThrows(CoreException.class,
                        () -> Product.builder()
                                .name(validName)
                                .description(validDescription)
                                .price(validPrice)
                                .status(null)
                                .isVisible(validIsVisible)
                                .isSellable(validIsSellable)
                                .build()
                );

                // assert
                assertEquals(ErrorType.BAD_REQUEST, result.getErrorType());
                assertEquals("Product : status 가 비어있을 수 없습니다.", result.getCustomMessage());
            }
        }

        @DisplayName("Product isVisible 유효성 검사")
        @Nested
        class IsVisibleTest {

            @DisplayName("실패 케이스: isVisible이 null이면 Product 객체 생성 실패")
            @Test
            void createProduct_withNullIsVisible_BadRequest() {
                // arrange & act
                CoreException result = assertThrows(CoreException.class,
                        () -> Product.builder()
                                .name(validName)
                                .description(validDescription)
                                .price(validPrice)
                                .status(validStatus)
                                .isVisible(null)
                                .isSellable(validIsSellable)
                                .build()
                );

                // assert
                assertEquals(ErrorType.BAD_REQUEST, result.getErrorType());
                assertEquals("Product : isVisible 가 비어있을 수 없습니다.", result.getCustomMessage());
            }
        }

        @DisplayName("Product isSellable 유효성 검사")
        @Nested
        class IsSellableTest {

            @DisplayName("실패 케이스: isSellable이 null이면 Product 객체 생성 실패")
            @Test
            void createProduct_withNullIsSellable_BadRequest() {
                // arrange & act
                CoreException result = assertThrows(CoreException.class,
                        () -> Product.builder()
                                .name(validName)
                                .description(validDescription)
                                .price(validPrice)
                                .status(validStatus)
                                .isVisible(validIsVisible)
                                .isSellable(null)
                                .build()
                );

                // assert
                assertEquals(ErrorType.BAD_REQUEST, result.getErrorType());
                assertEquals("Product : isSellable 가 비어있을 수 없습니다.", result.getCustomMessage());
            }
        }

        @DisplayName("Product description은 nullable")
        @Nested
        class DescriptionTest {

            @DisplayName("성공 케이스: description이 null이어도 Product 객체 생성 성공")
            @Test
            void createProduct_withNullDescription_Success() {
                // arrange & act
                Product product = Product.builder()
                        .name(validName)
                        .description(null)
                        .price(validPrice)
                        .status(validStatus)
                        .isVisible(validIsVisible)
                        .isSellable(validIsSellable)
                        .build();

                // assert
                assertNotNull(product);
                assertNull(product.getDescription());
            }
        }
    }
}

