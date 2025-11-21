package com.loopers.application.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductDomainService;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.stock.Stock;
import com.loopers.domain.stock.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class ProductFacade {
    private final ProductService productService;
    private final BrandService brandService;
    private final StockService stockService;
    private final ProductDomainService productDomainService;

    public Page<ProductInfo> getProducts(Long brandId, Pageable pageable, String sort) {
        Page<Product> products = productService.getProducts(brandId, pageable, sort);
        return products.map(ProductInfo::from);
    }

    @Transactional(readOnly = true)
    public ProductDetailInfo getProductDetail(Long productId) {
        Product p = productService.getProduct(productId);
        Stock s = stockService.getStockByProductId(productId);
        Brand b = brandService.getBrand(p.getBrandId());
        return productDomainService.createProductDetail(p, s, b);
    }
}
