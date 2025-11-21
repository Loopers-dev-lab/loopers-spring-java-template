package com.loopers.domain.product;

import com.loopers.application.product.ProductDetailInfo;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.stock.Stock;
import org.springframework.stereotype.Service;

@Service
public class ProductDomainService {
    public ProductDetailInfo createProductDetail(Product product, Stock stock, Brand brand) {
        return ProductDetailInfo.of(product, stock, brand);
    }
}
