package com.loopers.application.like;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.loopers.domain.brand.BrandService;
import com.loopers.domain.like.LikeService;
import com.loopers.domain.product.LikeCount;
import com.loopers.domain.product.Price;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.Stock;
import com.loopers.domain.user.LoginId;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
class LikeFacadeIntegrationTest {

    @Autowired
    private LikeFacade likeFacade;

    @MockitoSpyBean
    private UserService userService;

    @MockitoSpyBean
    private LikeService likeService;

    @MockitoSpyBean
    private ProductService productService;

    @MockitoSpyBean
    private BrandService brandService;

    @DisplayName("좋아요를 등록할 때,")
    @Nested
    class RecordLike {

        @DisplayName("사용자와 상품이 존재하면 좋아요가 등록되고 좋아요 수가 증가한다.")
        @Test
        void recordsLike_whenUserAndProductExist() {
            // arrange
            String loginId = "testUser";
            Long userId = 1L;
            Long productId = 1L;
            LikeCommand.LikeProductCommand command = new LikeCommand.LikeProductCommand(loginId, productId);

            User user = createUser(userId, loginId);
            Product product = createProduct(productId, "상품명", 1L, 10000, 10, 100);
            Product increasedProduct = createProduct(productId, "상품명", 1L, 10000, 11, 100);

            doReturn(Optional.of(user)).when(userService).findUserByLoginId(loginId);
            doReturn(false).when(likeService).existsByUserIdAndProductId(userId, productId);
            doReturn(Optional.of(product)).when(productService).findProductById(productId);
            doReturn(increasedProduct).when(productService).increaseLikeCount(productId);

            // act
            LikeInfo result = likeFacade.recordLike(command);

            // assert
            assertAll(
                    () -> assertThat(result.productId()).isEqualTo(productId),
                    () -> assertThat(result.likeCount()).isEqualTo(11)
            );

            // verify
            verify(userService, times(1)).findUserByLoginId(loginId);
            verify(likeService, times(1)).existsByUserIdAndProductId(userId, productId);
            verify(productService, times(1)).increaseLikeCount(productId);
            verify(likeService, times(1)).recordLike(userId, productId);
        }

        @DisplayName("이미 좋아요가 등록되어 있으면 멱등하게 현재 상태를 반환한다.")
        @Test
        void returnsCurrentState_whenLikeAlreadyExists() {
            // arrange
            String loginId = "testUser";
            Long userId = 1L;
            Long productId = 1L;
            LikeCommand.LikeProductCommand command = new LikeCommand.LikeProductCommand(loginId, productId);

            User user = createUser(userId, loginId);
            Product product = createProduct(productId, "상품명", 1L, 10000, 10, 100);

            doReturn(Optional.of(user)).when(userService).findUserByLoginId(loginId);
            doReturn(true).when(likeService).existsByUserIdAndProductId(userId, productId);
            doReturn(Optional.of(product)).when(productService).findProductById(productId);

            // act
            LikeInfo result = likeFacade.recordLike(command);

            // assert
            assertAll(
                    () -> assertThat(result.productId()).isEqualTo(productId),
                    () -> assertThat(result.likeCount()).isEqualTo(10)
            );

            // verify
            verify(userService, times(1)).findUserByLoginId(loginId);
            verify(likeService, times(1)).existsByUserIdAndProductId(userId, productId);
            verify(productService, never()).increaseLikeCount(anyLong());
            verify(likeService, never()).recordLike(anyLong(), anyLong());
        }

        @DisplayName("사용자가 존재하지 않으면 NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsException_whenUserDoesNotExist() {
            // arrange
            String loginId = "nonExistentUser";
            Long productId = 1L;
            LikeCommand.LikeProductCommand command = new LikeCommand.LikeProductCommand(loginId, productId);

            doReturn(Optional.empty()).when(userService).findUserByLoginId(loginId);

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                likeFacade.recordLike(command);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
            assertThat(exception.getMessage()).contains("사용자를 찾을 수 없습니다.");

            // verify
            verify(userService, times(1)).findUserByLoginId(loginId);
            verify(likeService, never()).existsByUserIdAndProductId(anyLong(), anyLong());
            verify(productService, never()).increaseLikeCount(anyLong());
        }

        @DisplayName("상품이 존재하지 않으면 NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsException_whenProductDoesNotExist() {
            // arrange
            String loginId = "testUser";
            Long userId = 1L;
            Long productId = 999L;
            LikeCommand.LikeProductCommand command = new LikeCommand.LikeProductCommand(loginId, productId);

            User user = createUser(userId, loginId);

            doReturn(Optional.of(user)).when(userService).findUserByLoginId(loginId);
            doReturn(false).when(likeService).existsByUserIdAndProductId(userId, productId);
            doThrow(new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."))
                    .when(productService).increaseLikeCount(productId);

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                likeFacade.recordLike(command);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
            assertThat(exception.getMessage()).contains("상품을 찾을 수 없습니다.");

            // verify
            verify(userService, times(1)).findUserByLoginId(loginId);
            verify(likeService, times(1)).existsByUserIdAndProductId(userId, productId);
            verify(productService, times(1)).increaseLikeCount(productId);
            verify(likeService, never()).recordLike(anyLong(), anyLong());
        }
    }

    @DisplayName("좋아요를 취소할 때,")
    @Nested
    class CancelLike {

        @DisplayName("좋아요가 등록되어 있으면 좋아요가 취소되고 좋아요 수가 감소한다.")
        @Test
        void cancelsLike_whenLikeExists() {
            // arrange
            String loginId = "testUser";
            Long userId = 1L;
            Long productId = 1L;
            LikeCommand.LikeProductCommand command = new LikeCommand.LikeProductCommand(loginId, productId);

            User user = createUser(userId, loginId);
            Product product = createProduct(productId, "상품명", 1L, 10000, 10, 100);
            Product decreasedProduct = createProduct(productId, "상품명", 1L, 10000, 9, 100);

            doReturn(Optional.of(user)).when(userService).findUserByLoginId(loginId);
            doReturn(true).when(likeService).existsByUserIdAndProductId(userId, productId);
            doReturn(Optional.of(product)).when(productService).findProductById(productId);
            doReturn(decreasedProduct).when(productService).decreaseLikeCount(productId);

            // act
            LikeInfo result = likeFacade.cancelLike(command);

            // assert
            assertAll(
                    () -> assertThat(result.productId()).isEqualTo(productId),
                    () -> assertThat(result.likeCount()).isEqualTo(9)
            );

            // verify
            verify(userService, times(1)).findUserByLoginId(loginId);
            verify(likeService, times(1)).existsByUserIdAndProductId(userId, productId);
            verify(productService, times(1)).decreaseLikeCount(productId);
            verify(likeService, times(1)).cancelLike(userId, productId);
        }

        @DisplayName("이미 좋아요가 취소되어 있으면 멱등하게 현재 상태를 반환한다.")
        @Test
        void returnsCurrentState_whenLikeAlreadyCancelled() {
            // arrange
            String loginId = "testUser";
            Long userId = 1L;
            Long productId = 1L;
            LikeCommand.LikeProductCommand command = new LikeCommand.LikeProductCommand(loginId, productId);

            User user = createUser(userId, loginId);
            Product product = createProduct(productId, "상품명", 1L, 10000, 10, 100);

            doReturn(Optional.of(user)).when(userService).findUserByLoginId(loginId);
            doReturn(false).when(likeService).existsByUserIdAndProductId(userId, productId);
            doReturn(Optional.of(product)).when(productService).findProductById(productId);

            // act
            LikeInfo result = likeFacade.cancelLike(command);

            // assert
            assertAll(
                    () -> assertThat(result.productId()).isEqualTo(productId),
                    () -> assertThat(result.likeCount()).isEqualTo(10)
            );

            // verify
            verify(userService, times(1)).findUserByLoginId(loginId);
            verify(likeService, times(1)).existsByUserIdAndProductId(userId, productId);
            verify(productService, never()).decreaseLikeCount(anyLong());
            verify(likeService, never()).cancelLike(anyLong(), anyLong());
        }

        @DisplayName("사용자가 존재하지 않으면 NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsException_whenUserDoesNotExist() {
            // arrange
            String loginId = "nonExistentUser";
            Long productId = 1L;
            LikeCommand.LikeProductCommand command = new LikeCommand.LikeProductCommand(loginId, productId);

            doReturn(Optional.empty()).when(userService).findUserByLoginId(loginId);

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                likeFacade.cancelLike(command);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
            assertThat(exception.getMessage()).contains("사용자를 찾을 수 없습니다.");

            // verify
            verify(userService, times(1)).findUserByLoginId(loginId);
            verify(likeService, never()).existsByUserIdAndProductId(anyLong(), anyLong());
            verify(productService, never()).decreaseLikeCount(anyLong());
        }

        @DisplayName("상품이 존재하지 않으면 NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsException_whenProductDoesNotExist() {
            // arrange
            String loginId = "testUser";
            Long userId = 1L;
            Long productId = 999L;
            LikeCommand.LikeProductCommand command = new LikeCommand.LikeProductCommand(loginId, productId);

            User user = createUser(userId, loginId);

            doReturn(Optional.of(user)).when(userService).findUserByLoginId(loginId);
            doReturn(false).when(likeService).existsByUserIdAndProductId(userId, productId);
            doReturn(Optional.empty()).when(productService).findProductById(productId);

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                likeFacade.cancelLike(command);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
            assertThat(exception.getMessage()).contains("상품을 찾을 수 없습니다.");

            // verify
            verify(userService, times(1)).findUserByLoginId(loginId);
            verify(likeService, times(1)).existsByUserIdAndProductId(userId, productId);
            verify(productService, never()).decreaseLikeCount(anyLong());
        }
    }

    @DisplayName("내가 좋아요한 상품 목록을 조회할 때,")
    @Nested
    class GetLikedProducts {

        @DisplayName("좋아요한 상품이 있으면 상품 목록이 반환된다.")
        @Test
        void returnsProducts_whenLikedProductsExist() {
            // arrange
            String loginId = "testUser";
            Long userId = 1L;
            List<Long> productIds = List.of(1L, 2L);

            User user = createUser(userId, loginId);
            Product product1 = createProduct(1L, "상품1", 1L, 10000, 10, 100);
            Product product2 = createProduct(2L, "상품2", 2L, 20000, 20, 200);

            Map<Long, String> brandNamesMap = new HashMap<>();
            brandNamesMap.put(1L, "브랜드1");
            brandNamesMap.put(2L, "브랜드2");

            doReturn(Optional.of(user)).when(userService).findUserByLoginId(loginId);
            doReturn(productIds).when(likeService).findLikedProductIds(userId);
            doReturn(Optional.of(product1)).when(productService).findProductById(1L);
            doReturn(Optional.of(product2)).when(productService).findProductById(2L);
            doReturn(brandNamesMap).when(brandService).findBrandNamesByIds(any());

            // act
            List<com.loopers.application.product.ProductInfo> result = likeFacade.getLikedProducts(loginId);

            // assert
            assertThat(result).hasSize(2);
            assertAll(
                    () -> assertThat(result.get(0).id()).isEqualTo(1L),
                    () -> assertThat(result.get(0).name()).isEqualTo("상품1"),
                    () -> assertThat(result.get(0).brandName()).isEqualTo("브랜드1"),
                    () -> assertThat(result.get(1).id()).isEqualTo(2L),
                    () -> assertThat(result.get(1).name()).isEqualTo("상품2"),
                    () -> assertThat(result.get(1).brandName()).isEqualTo("브랜드2")
            );

            // verify
            verify(userService, times(1)).findUserByLoginId(loginId);
            verify(likeService, times(1)).findLikedProductIds(userId);
            verify(brandService, times(1)).findBrandNamesByIds(any());
        }

        @DisplayName("좋아요한 상품이 없으면 NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsException_whenNoLikedProducts() {
            // arrange
            String loginId = "testUser";
            Long userId = 1L;

            User user = createUser(userId, loginId);

            doReturn(Optional.of(user)).when(userService).findUserByLoginId(loginId);
            doReturn(List.of()).when(likeService).findLikedProductIds(userId);

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                likeFacade.getLikedProducts(loginId);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
            assertThat(exception.getMessage()).contains("좋아요한 상품이 없습니다.");

            // verify
            verify(userService, times(1)).findUserByLoginId(loginId);
            verify(likeService, times(1)).findLikedProductIds(userId);
            verify(productService, never()).findProductById(anyLong());
            verify(brandService, never()).findBrandNamesByIds(any());
        }

        @DisplayName("사용자가 존재하지 않으면 NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsException_whenUserDoesNotExist() {
            // arrange
            String loginId = "nonExistentUser";

            doReturn(Optional.empty()).when(userService).findUserByLoginId(loginId);

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                likeFacade.getLikedProducts(loginId);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
            assertThat(exception.getMessage()).contains("사용자를 찾을 수 없습니다.");

            // verify
            verify(userService, times(1)).findUserByLoginId(loginId);
            verify(likeService, never()).findLikedProductIds(anyLong());
        }
    }

    private User createUser(Long id, String loginId) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getLoginId()).thenReturn(new LoginId(loginId));
        return user;
    }

    private Product createProduct(Long id, String name, Long brandId, Integer price, Integer likeCount, Integer stock) {
        Product product = mock(Product.class);
        Price priceValue = mock(Price.class);
        LikeCount likeCountValue = mock(LikeCount.class);
        Stock stockValue = mock(Stock.class);

        when(product.getId()).thenReturn(id);
        when(product.getName()).thenReturn(name);
        when(product.getBrandId()).thenReturn(brandId);
        when(product.getPrice()).thenReturn(priceValue);
        when(product.getLikeCount()).thenReturn(likeCountValue);
        when(product.getStock()).thenReturn(stockValue);
        when(priceValue.getPrice()).thenReturn(price);
        when(likeCountValue.getCount()).thenReturn(likeCount);
        when(stockValue.getQuantity()).thenReturn(stock);

        return product;
    }
}
