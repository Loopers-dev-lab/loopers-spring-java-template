package com.loopers.domain.product;

import com.loopers.domain.Money;
import com.loopers.domain.brand.Brand;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@RequiredArgsConstructor
@Component
public class ProductService {

    private final ProductRepository productRepository;

    public Product registerProduct(String productCode, String productName, BigDecimal price, int stock, Brand brand) {
        if (productCode == null || productCode.trim().isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품 코드는 필수값입니다");
        }
        validateProductCodeNotDuplicated(productCode);

        Product product = Product.createProduct(productCode, productName, Money.of(price), stock, brand);
        return productRepository.registerProduct(product);
    }

    public List<Product> getProducts(String productName, Long brandId, int page, int size, ProductSortType sortType) {

        ProductSortType appliedSortType = (sortType != null) ? sortType : ProductSortType.LATEST;

        // Specification 조합으로 동적 쿼리 생성
        // 페이징 사용 시 fetch join은 제외 (페이징과 fetch join은 함께 사용 불가)
        Specification<Product> spec = Specification
                .where(ProductSpecification.isNotDeleted())
                .and(ProductSpecification.hasProductName(productName))
                .and(ProductSpecification.hasBrandId(brandId));

        return productRepository.findAll(spec, page, size, appliedSortType);
    }

    public Product getProductDetail(Long productId) {
        return productRepository.findByIdWithBrand(productId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다"));
    }

    private void validateProductCodeNotDuplicated(String productCode) {
        if (productRepository.existsProductCode(productCode)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "중복된 상품 코드 오류");
        }
    }

    public Product getProductWithLock(Long productId) {
        return productRepository.findByIdWithLock(productId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품 정보가 없습니다"));
    }

    @Transactional
    public Product updateProduct(Long productId, String productName, BigDecimal price) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다"));

        Money moneyPrice = price != null ? Money.of(price) : null;
        product.updateProductInfo(productName, moneyPrice);

        return product;
    }
}
