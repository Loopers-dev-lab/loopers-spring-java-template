package com.loopers.domain.brand;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "brand")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Brand extends BaseEntity {

  private static final int MAX_NAME_LENGTH = 50;

  @Column(nullable = false, length = MAX_NAME_LENGTH)
  private String name;

  @Column(length = 500)
  private String description;

  private Brand(String name, String description) {
    validateName(name);
    this.name = name;
    this.description = description;
  }

  public static Brand of(String name, String description) {
    return new Brand(name, description);
  }

  public static Brand of(String name) {
    return new Brand(name, null);
  }

  private void validateName(String name) {
    if (name == null || name.isBlank()) {
      throw new CoreException(ErrorType.INVALID_BRAND_NAME_EMPTY);
    }
    if (name.length() > MAX_NAME_LENGTH) {
      throw new CoreException(ErrorType.INVALID_BRAND_NAME_LENGTH);
    }
  }

  public boolean isSameId(Long otherId) {
    return this.getId().equals(otherId);
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }
}
