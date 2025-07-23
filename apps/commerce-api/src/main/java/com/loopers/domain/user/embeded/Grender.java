package com.loopers.domain.user.embeded;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Embeddable;
import lombok.Getter;

import java.util.Arrays;

@Embeddable
@Getter
public class Grender {
    
    public enum GenderType {
        M, F
    }
    
    private String grender;

    public Grender() {
    }
    
    public Grender(String grender) {
        boolean isValid = Arrays.stream(GenderType.values())
                .anyMatch(type -> type.name().equals(grender.toUpperCase()));
        
        if (!isValid) {
            throw new CoreException(ErrorType.BAD_REQUEST, "성별은 'M' 또는 'F'만 입력 가능합니다.");
        }
        
        this.grender = grender.toUpperCase();
    }
}
