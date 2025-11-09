package com.loopers.core.service.product;

import com.loopers.core.domain.brand.vo.BrandId;
import com.loopers.core.domain.common.type.OrderSort;
import com.loopers.core.domain.common.vo.PageNo;
import com.loopers.core.domain.common.vo.PageSize;
import com.loopers.core.domain.product.Product;
import com.loopers.core.domain.product.ProductListView;
import com.loopers.core.domain.product.repository.ProductRepository;
import com.loopers.core.domain.product.vo.ProductId;
import com.loopers.core.service.product.query.GetProductListQuery;
import com.loopers.core.service.product.query.GetProductQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductQueryService {

    private final ProductRepository productRepository;

    public Product getProductBy(GetProductQuery query) {
        return productRepository.getById(new ProductId(query.getProductId()));

    }

    public ProductListView getProductList(GetProductListQuery query) {
        return productRepository.findListWithCondition(
                new BrandId(query.getBrandId()),
                OrderSort.from(query.getCreatedAtSort()),
                OrderSort.from(query.getPriceSort()),
                OrderSort.from(query.getLikeCountSort()),
                new PageNo(query.getPageNo()),
                new PageSize(query.getPageSize())
        );
    }
}
