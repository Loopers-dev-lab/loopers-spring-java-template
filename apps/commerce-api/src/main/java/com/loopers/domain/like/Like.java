package com.loopers.domain.like;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.product.Product;
import com.loopers.domain.user.User;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "likes", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"ref_user_id", "ref_product_id"})
})
@Getter
public class Like extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ref_user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ref_product_id", nullable = false)
  private Product product;

  protected Like() {
  }

  private Like(User user, Product product) {
    this.user = user;
    this.product = product;
  }

  public static Like create(User user, Product product) {
    if (user == null || user.getId() == null) {
      throw new CoreException(ErrorType.BAD_REQUEST, "사용자ID는 비어있을 수 없습니다.");
    }
    if (product == null || product.getId() == null) {
      throw new CoreException(ErrorType.BAD_REQUEST, "상품ID는 비어있을 수 없습니다.");
    }
    return new Like(user, product);
  }

}
