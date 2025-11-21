package com.loopers.domain.product;

import com.loopers.domain.product.command.ProductSearchFilter;
import com.loopers.domain.product.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.loopers.support.TestEntityUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class InMemoryProductRepository implements ProductRepository {

    private final Map<Long, Product> store = new HashMap<>();
    private long sequence = 0L;

    @Override
    public Optional<Product> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Product> findByIdIn(List<Long> ids) {
        return ids.stream()
                .map(this::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    @Override
    public Page<Product> findAll(ProductSearchFilter condition, Pageable pageable) {
        List<Product> allProducts = store.values().stream().toList();
        return new PageImpl<>(allProducts, pageable, allProducts.size());
    }

    @Override
    public Product save(Product product) {
        if (product.getId() == null) {
            // BaseEntity의 id는 final이므로 리플렉션을 사용하거나 다른 방법 필요
            // 테스트용으로 간단히 처리
            Long newId = ++sequence;
            Product newProduct = new Product(
                    product.getBrandId(),
                    product.getName(),
                    product.getDescription(),
                    product.getPrice(),
                    product.getStock()
            );
            store.put(newId, newProduct);
            return newProduct;
        } else {
            store.put(product.getId(), product);
            return product;
        }
    }

    @Override
    public int decreaseStock(Long productId, int quantity) {
        Product product = store.get(productId);
        if (product == null) {
            return 0;
        }
        if (!product.isStockAvailable(quantity)) {
            return 0;
        }
        product.decreaseStock(quantity);
        return 1;
    }

    @Override
    public int incrementLikeCount(Long productId) {
        Product product = store.get(productId);
        if (product == null) {
            return 0;
        }
        product.increaseLikeCount();
        return 1;
    }

    @Override
    public int decrementLikeCount(Long productId) {
        Product product = store.get(productId);
        if (product == null || product.getLikeCount() == 0) {
            return 0;
        }
        product.decreaseLikeCount();
        return 1;
    }

    public void clear() {
        store.clear();
        sequence = 0L;
    }

    public Product saveWithId(Long id, Product product) {
        Product productWithId = TestEntityUtils.setIdWithNow(product, id);
        store.put(id, productWithId);
        if (id > sequence) {
            sequence = id;
        }
        return productWithId;
    }
}