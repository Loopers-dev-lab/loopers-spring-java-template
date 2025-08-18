package com.loopers.config;

import com.loopers.application.product.ProductCommand;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Component("productKeyGenerator")
public class CacheKeyGenerator implements KeyGenerator {
    
    @Override
    public Object generate(Object target, Method method, Object... params) {
        if (params.length == 1 && params[0] instanceof ProductCommand.Request.GetList request) {
            return generateProductListKey(request);
        }
        if (params.length == 1 && params[0] instanceof Long productId) {
            return "product:" + productId;
        }
        
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(method.getName());
        for (Object param : params) {
            keyBuilder.append(":").append(param != null ? param.toString() : "null");
        }
        return keyBuilder.toString();
    }
    
    private String generateProductListKey(ProductCommand.Request.GetList request) {
        return String.format("list:%s:%s:%d:%d",
                request.brandId() != null ? request.brandId() : "all",
                request.sort() != null ? request.sort() : "default",
                request.page(),
                request.size()
        );
    }
}
