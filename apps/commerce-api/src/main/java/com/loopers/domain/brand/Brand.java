package com.loopers.domain.brand;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "brand")
@Getter
public class Brand extends BaseEntity {
  private String name;
  private String story;

  protected Brand() {
    this.name = "";
    this.story = "";
  }

  private Brand(String name, String story) {
    this.name = name;
    this.story = story;
  }

  public static Brand create(String name, String story) {
    if (name == null || name.isBlank()) {
      throw new CoreException(ErrorType.BAD_REQUEST, "브랜드명은 비어있을 수 없습니다.");
    }
    return new Brand(name, story);
  }

}
