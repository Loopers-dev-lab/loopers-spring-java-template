package com.loopers.interfaces.api.argumentresolver

import com.loopers.domain.user.UserInfo
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.core.MethodParameter
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

class UserInfoArgumentResolver: HandlerMethodArgumentResolver {
    companion object {
        private const val KEY_USER_ID = "X-USER-ID"
    }

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return UserInfo::class.java.isAssignableFrom(parameter.parameterType)
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): UserInfo {
        val userId = webRequest.getHeader(KEY_USER_ID)
            ?: throw CoreException(ErrorType.BAD_REQUEST, "유저 ID 헤더는 필수입니다.")

        return UserInfo(userId)
    }
}
