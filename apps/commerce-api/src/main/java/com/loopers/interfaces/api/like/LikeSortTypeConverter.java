package com.loopers.interfaces.api.like;

import com.loopers.domain.productlike.LikeSortType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class LikeSortTypeConverter implements Converter<String, LikeSortType> {

  @Override
  public LikeSortType convert(String source) {
    return LikeSortType.from(source);
  }
}
