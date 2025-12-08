package com.loopers.core.domain.brand;

import com.loopers.core.domain.brand.vo.BrandDescription;
import com.loopers.core.domain.brand.vo.BrandId;
import com.loopers.core.domain.brand.vo.BrandName;
import org.instancio.Instancio;

import static org.instancio.Select.field;

public class BrandFixture {

    public static Brand create() {
        return Instancio.of(Brand.class)
                .set(field(Brand::getId), BrandId.empty())
                .set(field(Brand::getName), new BrandName("테스트 브랜드"))
                .set(field(Brand::getDescription), new BrandDescription("테스트 브랜드 설명"))
                .create();
    }

    public static Brand createWith(String name) {
        return Instancio.of(Brand.class)
                .set(field(Brand::getId), BrandId.empty())
                .set(field(Brand::getName), new BrandName(name))
                .set(field(Brand::getDescription), new BrandDescription("테스트 브랜드 설명"))
                .create();
    }

    public static Brand createWith(BrandName brandName, BrandDescription brandDescription) {
        return Instancio.of(Brand.class)
                .set(field(Brand::getId), BrandId.empty())
                .set(field(Brand::getName), brandName)
                .set(field(Brand::getDescription), brandDescription)
                .create();
    }
}
