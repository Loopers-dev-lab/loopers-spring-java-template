package com.loopers.interfaces.api.signup;

import com.loopers.application.signup.SignUpFacade;
import com.loopers.application.signup.SignUpInfo;
import com.loopers.interfaces.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회원가입 API v1 컨트롤러.
 * <p>
 * 사용자 회원가입 요청을 처리하는 REST API를 제공합니다.
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/signup")
public class SignUpV1Controller {

    private final SignUpFacade signUpFacade;

    /**
     * 회원가입을 처리합니다.
     *
     * @param request 회원가입 요청 데이터 (userId, email, birthDate, gender)
     * @return 생성된 사용자 정보를 담은 API 응답
     * @throws CoreException gender 값이 유효하지 않거나, 유효성 검증 실패 또는 중복 ID 존재 시
     */
    @PostMapping
    public ApiResponse<SignUpV1Dto.SignupResponse> signUp(
        @Valid @RequestBody SignUpV1Dto.SignUpRequest request
    ) {
        SignUpInfo info = signUpFacade.signUp(
            request.userId(),
            request.email(),
            request.birthDate(),
            request.gender()
        );
        SignUpV1Dto.SignupResponse response = SignUpV1Dto.SignupResponse.from(info);
        return ApiResponse.success(response);
    }
}
