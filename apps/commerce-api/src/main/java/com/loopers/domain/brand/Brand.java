package com.loopers.domain.brand;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "brands")
public class Brand extends BaseEntity {

    private String name;
    private boolean active = true;

    protected Brand() {
    }

    private Brand(String name) {
        this.name = name;
    }

    public static Brand create(String name) {
        return new Brand(name);
    }

    public static Brand createInactive(String name) {
        Brand brand = new Brand(name);
        brand.deactivate();
        return brand;
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    public boolean isActive() {
        return active;
    }

    public String getName() {
        return name;
    }
}
