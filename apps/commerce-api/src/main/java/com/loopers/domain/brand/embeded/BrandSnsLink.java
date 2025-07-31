package com.loopers.domain.brand.embeded;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Embeddable;
import lombok.Getter;

@Embeddable
@Getter
public class BrandSnsLink {
    private String BrandSnsLick;

    private static final String urlPattern = "^https?:\\/\\/[a-zA-Z0-9\\-._~:/?#\\[\\]@!$&'()*+,;=%]+$";

    private BrandSnsLink(String brandSnsLick) {
        BrandSnsLick = brandSnsLick;
    }

    public BrandSnsLink() {

    }

    public static BrandSnsLink of(String bradnSnsLink) {
        if(!bradnSnsLink.matches(urlPattern)){
            throw new CoreException(ErrorType.BAD_REQUEST, "sns 링크를 확인해주세요");
        }
        return new BrandSnsLink(bradnSnsLink);
    }
}
