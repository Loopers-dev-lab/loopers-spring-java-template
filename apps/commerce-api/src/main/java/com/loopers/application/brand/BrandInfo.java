package com.loopers.application.brand;

import com.loopers.domain.brand.Brand;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

public record BrandInfo(Long id, String name, String story) {
  public static BrandInfo from(Brand model) {
    if (model == null) throw new CoreException(ErrorType.NOT_FOUND, "유저정보를 찾을수 없습니다.");
    return new BrandInfo(
        model.getId(),
        model.getName(),
        model.getStory()
    );
  }
}
