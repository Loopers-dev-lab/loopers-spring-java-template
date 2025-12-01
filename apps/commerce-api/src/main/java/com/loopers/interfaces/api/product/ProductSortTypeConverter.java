package com.loopers.interfaces.api.product;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class ProductSortTypeConverter implements Converter<String, ProductSortType> {

  @Override
  public ProductSortType convert(String source) {
    return ProductSortType.from(source);
  }
}
