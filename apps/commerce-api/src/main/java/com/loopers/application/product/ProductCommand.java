package com.loopers.application.product;

public class ProductCommand {
    public class Request{
        public record GetList(
                Long brandId,
                String sort,
                int page,
                int size
        ){
        }
    }

}
