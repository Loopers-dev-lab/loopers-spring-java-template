package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

/**
 * packageName : com.loopers.domain.product
 * fileName     : ProductService
 * author      : byeonsungmun
 * date        : 2025. 11. 12.
 * description :
 * ===========================================
 * DATE         AUTHOR       NOTE
 * -------------------------------------------
 * 2025. 11. 12.     byeonsungmun       최초 생성
 */

@Component
@RequiredArgsConstructor
public class ProductService {

    private static final Duration TTL_LIST = Duration.ofMinutes(10);
    private static final Duration TTL_DETAIL = Duration.ofMinutes(5);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ProductRepository productRepository;


    @Transactional(readOnly = true)
    public Page<Product> getProducts(Long brandId, Pageable sortedPageable) {
        String key = "product:list:"
                + (brandId == null ? "all" : brandId) + ":"
                + sortedPageable.getPageNumber() + ":"
                + sortedPageable.getPageSize();

        try {
            Page<Product> cached = (Page<Product>) redisTemplate.opsForValue().get(key);
            if (cached != null) {
                return cached;
            }
        } catch (Exception e) {
            return (brandId == null)
                    ? productRepository.findAll(sortedPageable)
                    : productRepository.findByBrandId(brandId, sortedPageable);
        }

        Page<Product> products = (brandId == null)
                ? productRepository.findAll(sortedPageable)
                : productRepository.findByBrandId(brandId, sortedPageable);

        try {
            redisTemplate.opsForValue().set(key, products, TTL_LIST);
        } catch (Exception ignored) {
        }

        return products;
    }

    public Product getProduct(Long productId) {
        String key = "product:detail:" + productId;

        try {
            Product cached = (Product) redisTemplate.opsForValue().get(key);
            if (cached != null) {
                return cached;
            }
        } catch (Exception e) {
            return productRepository.findById(productId)
                    .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "해당 상품이 없습니다"));
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "해당 상품이 없습니다"));

        try {
            redisTemplate.opsForValue().set(key, product, TTL_DETAIL);
        } catch (Exception ignored) {
        }

        return product;
    }
}
