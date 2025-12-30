package com.loopers.domain.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "product")
@Getter
public class ProductInventory {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "ref_brand_id")
    private Long brandId;

    @Column(nullable = false)
    private Long stock;

    protected ProductInventory() {
    }
}
