package com.loopers.core.domain.product;

import com.loopers.core.domain.brand.vo.BrandId;
import com.loopers.core.domain.common.vo.DeletedAt;
import com.loopers.core.domain.product.vo.*;
import org.instancio.Instancio;

import java.math.BigDecimal;

import static org.instancio.Select.field;

public class ProductFixture {

    public static Product create() {
        return Instancio.of(Product.class)
                .set(field(Product::getId), ProductId.empty())
                .set(field(Product::getBrandId), Instancio.create(BrandId.class))
                .set(field(Product::getName), new ProductName("테스트 상품"))
                .set(field(Product::getPrice), new ProductPrice(new BigDecimal(10000)))
                .set(field(Product::getStock), new ProductStock(100L))
                .set(field(Product::getLikeCount), ProductLikeCount.init())
                .set(field(Product::getDeletedAt), DeletedAt.empty())
                .create();
    }

    public static Product createWith(BrandId brandId) {
        return Instancio.of(Product.class)
                .set(field(Product::getId), ProductId.empty())
                .set(field(Product::getBrandId), brandId)
                .set(field(Product::getName), new ProductName("테스트 상품"))
                .set(field(Product::getPrice), new ProductPrice(new BigDecimal(10000)))
                .set(field(Product::getStock), new ProductStock(100L))
                .set(field(Product::getLikeCount), ProductLikeCount.init())
                .set(field(Product::getDeletedAt), DeletedAt.empty())
                .create();
    }

    public static Product createWith(BrandId brandId, ProductName name, ProductPrice price, ProductStock stock) {
        return Instancio.of(Product.class)
                .set(field(Product::getId), ProductId.empty())
                .set(field(Product::getBrandId), brandId)
                .set(field(Product::getName), name)
                .set(field(Product::getPrice), price)
                .set(field(Product::getStock), stock)
                .set(field(Product::getLikeCount), ProductLikeCount.init())
                .set(field(Product::getDeletedAt), DeletedAt.empty())
                .create();
    }

    public static Product createWith(BrandId brandId, ProductLikeCount likeCount) {
        return Instancio.of(Product.class)
                .set(field(Product::getId), ProductId.empty())
                .set(field(Product::getBrandId), brandId)
                .set(field(Product::getName), new ProductName("테스트 상품"))
                .set(field(Product::getPrice), new ProductPrice(new BigDecimal(10000)))
                .set(field(Product::getStock), new ProductStock(100L))
                .set(field(Product::getLikeCount), likeCount)
                .set(field(Product::getDeletedAt), DeletedAt.empty())
                .create();
    }

    public static Product createWith(ProductId productId, BrandId brandId, ProductName name, ProductPrice price, ProductStock stock) {
        return Instancio.of(Product.class)
                .set(field(Product::getId), productId)
                .set(field(Product::getBrandId), brandId)
                .set(field(Product::getName), name)
                .set(field(Product::getPrice), price)
                .set(field(Product::getStock), stock)
                .set(field(Product::getLikeCount), ProductLikeCount.init())
                .set(field(Product::getDeletedAt), DeletedAt.empty())
                .create();
    }
}
