package com.loopers.support.interceptor;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AccessControlInterceptor implements HandlerInterceptor {

    // private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private static final String ENDPOINT = "/api/v1";

    public boolean preHandle(
            HttpServletRequest request
            , HttpServletResponse response
            , Object handler
    ) throws Exception {

        // 헤더에 X-USER-ID 가 없으면 BAD REQUEST
        String requestPath = request.getRequestURI().substring(request.getContextPath().length());
        String userIdHeader = request.getHeader("X-USER-ID");

        if(userIdHeader == null || userIdHeader.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, requestPath + " API 요청에 X-USER-ID 가 꼭 필요합니다.");
        }

        // String을 Long으로 파싱하여 유효성 검사
        try {
            Long.parseLong(userIdHeader);
        } catch (NumberFormatException e) {
            throw new CoreException(ErrorType.BAD_REQUEST, requestPath + " API 요청에 X-USER-ID 는 숫자(Long) 형식이어야 합니다.");
        }

        return true;
    }
}
