package com.loopers.application.product;

import com.loopers.domain.brand.BrandModel;
import com.loopers.domain.product.ProductModel;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;

public class ProductCommand {

    public class Request{
        public record GetList(
                Long brandId,
                String sort,
                int page,
                int size
        ){
        }
        public record Get(
                Long productId
        ){
        }
    }

    public record ProductData(
            Page<ProductModel> productModels,
            List<ProductItem> productItemList
    ){
        public record ProductItem(
                Long productId,
                String name,
                BigDecimal price,
                Long brandId,
                String brandName,
                String imgUrl,
                BigDecimal likeCount,
                String status,
                BigDecimal stock
        ){
            public static ProductItem of(ProductModel productModel, BrandModel brandModel) {
                return new ProductItem(
                        productModel.getId(),
                        productModel.getProductName().getValue(),
                        productModel.getPrice().getValue(),
                        brandModel.getId(),
                        brandModel.getBrandNaem().getValue(),
                        productModel.getImgUrl().getValue(),
                        productModel.getLikeCount().getValue(),
                        productModel.getStatus().getValue(),
                        productModel.getStock().getValue()
                );
            }

        }
    }

}
