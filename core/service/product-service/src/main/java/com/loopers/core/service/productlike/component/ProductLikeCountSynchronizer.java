package com.loopers.core.service.productlike.component;

import com.loopers.core.domain.product.Product;
import com.loopers.core.domain.product.repository.ProductRepository;
import com.loopers.core.domain.product.vo.ProductId;
import com.loopers.core.domain.product.vo.ProductLikeCount;
import com.loopers.core.domain.productlike.ProductLikeCache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class ProductLikeCountSynchronizer {

    private final ProductRepository productRepository;

    public void sync(List<ProductLikeCache> cachedLike, List<ProductLikeCache> cachedUnlike) {
        Map<ProductId, List<ProductLikeCache>> likedByProduct = cachedLike.stream()
                .collect(Collectors.groupingBy(ProductLikeCache::productId));
        Map<ProductId, List<ProductLikeCache>> unlikedByProduct = cachedUnlike.stream()
                .collect(Collectors.groupingBy(ProductLikeCache::productId));
        List<ProductId> productIds = Stream.concat(cachedLike.stream(), cachedUnlike.stream())
                .map(ProductLikeCache::productId)
                .toList();

        List<Product> updatedProducts = productRepository.findAllByIdIn(productIds).stream()
                .map(product -> {
                    long addedLikes = likedByProduct.getOrDefault(product.getId(), List.of()).size();
                    long removedLikes = unlikedByProduct.getOrDefault(product.getId(), List.of()).size();
                    long newLikeCount = product.getLikeCount().value() + addedLikes - removedLikes;

                    return product.withLikeCount(new ProductLikeCount(newLikeCount));
                })
                .toList();

        productRepository.bulkSaveOrUpdate(updatedProducts);
    }
}
