package com.loopers.domain.product;

import com.loopers.domain.brand.Brand;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public Optional<Product> saveProduct(Product product) {
        return productRepository.save(product);
    }

    public Optional<Product> findById(Long productId) {
        return productRepository.findById(productId);
    }

    public Page<Product> findProducts(ProductCondition condition, Pageable pageable) {
        return productRepository.findProducts(condition, pageable);
    }

    /**
     * 상품과 브랜드 정보를 조합하여 조회
     * 도메인 서비스에서 Product + Brand 조합 로직 처리
     * Repository에서 fetch join을 통해 Brand를 함께 조회
     */
    public ProductWithBrand getProductDetailWithBrand(Long productId) {
        Product product = productRepository.findByIdWithBrand(productId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND,
                        "[productId = " + productId + "] Product를 찾을 수 없습니다."
                ));

        Brand brand = product.getBrand();
        if (brand == null) {
            throw new CoreException(
                    ErrorType.NOT_FOUND,
                    "[productId = " + productId + "] Product에 Brand 정보가 없습니다."
            );
        }

        return new ProductWithBrand(product, brand);
    }

    /**
     * Product와 Brand를 조합한 도메인 객체
     */
    public static class ProductWithBrand {
        private final Product product;
        private final Brand brand;

        public ProductWithBrand(Product product, Brand brand) {
            this.product = product;
            this.brand = brand;
        }

        public Product getProduct() {
            return product;
        }

        public Brand getBrand() {
            return brand;
        }
    }
}
