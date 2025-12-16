package com.loopers.domain.like;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductLikeSyncServiceTest {

    @Mock
    private ProductLikeRepository productLikeRepository;

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductLikeSyncService productLikeSyncService;

    @Test
    @DisplayName("좋아요 수가 불일치하면 동기화한다")
    void syncProductLikeCount_whenMismatch_thenSync() {
        // given
        Long productId = 1L;
        Product product = mock(Product.class);
        when(product.getLikeCount()).thenReturn(100L);
        when(productService.getProductById(productId)).thenReturn(product);
        when(productLikeRepository.countByProduct(product)).thenReturn(105L);

        // when
        productLikeSyncService.syncProductLikeCount(productId);

        // then
        verify(product).syncLikeCount(105L);
    }

    @Test
    @DisplayName("좋아요 수가 일치하면 동기화하지 않는다")
    void syncProductLikeCount_whenMatch_thenNoSync() {
        // given
        Long productId = 1L;
        Product product = mock(Product.class);
        when(product.getLikeCount()).thenReturn(100L);
        when(productService.getProductById(productId)).thenReturn(product);
        when(productLikeRepository.countByProduct(product)).thenReturn(100L);

        // when
        productLikeSyncService.syncProductLikeCount(productId);

        // then
        verify(product, never()).syncLikeCount(anyLong());
    }

    @Test
    @DisplayName("여러 상품의 좋아요 수를 동기화한다")
    void syncAllProductLikeCounts_success() {
        // given
        List<Long> productIds = List.of(1L, 2L, 3L);
        when(productLikeRepository.findDistinctProductIds()).thenReturn(productIds);

        Product product1 = mock(Product.class);
        Product product2 = mock(Product.class);
        Product product3 = mock(Product.class);

        when(productService.getProductById(1L)).thenReturn(product1);
        when(productService.getProductById(2L)).thenReturn(product2);
        when(productService.getProductById(3L)).thenReturn(product3);

        when(product1.getLikeCount()).thenReturn(10L);
        when(product2.getLikeCount()).thenReturn(20L);
        when(product3.getLikeCount()).thenReturn(30L);

        when(productLikeRepository.countByProduct(product1)).thenReturn(15L);
        when(productLikeRepository.countByProduct(product2)).thenReturn(20L);
        when(productLikeRepository.countByProduct(product3)).thenReturn(35L);

        // when
        int syncedCount = productLikeSyncService.syncAllProductLikeCounts();

        // then
        assertThat(syncedCount).isEqualTo(3);
        verify(product1).syncLikeCount(15L);
        verify(product2, never()).syncLikeCount(anyLong());
        verify(product3).syncLikeCount(35L);
    }
}
