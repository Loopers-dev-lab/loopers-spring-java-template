package com.loopers.interfaces.api.signup;

import com.loopers.application.signup.SignUpInfo;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.domain.user.UserService;
import com.loopers.interfaces.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import java.util.Locale;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/signup")
public class SignUpV1Controller {

    private final UserService userService;

    @PostMapping
    public ApiResponse<SignUpV1Dto.SignupResponse> signUp(
        @Valid @RequestBody SignUpV1Dto.SignUpRequest request
    ) {
        Gender gender;
        try {
            String genderValue = request.gender().trim().toUpperCase(Locale.ROOT);
            gender = Gender.valueOf(genderValue);
        } catch (IllegalArgumentException e) {
            throw new CoreException(ErrorType.BAD_REQUEST, "gender 값이 올바르지 않습니다.");
        }

        User user = userService.signUp(request.userId(), request.email(), request.birthDate(), gender);
        SignUpInfo info = SignUpInfo.from(user);
        SignUpV1Dto.SignupResponse response = SignUpV1Dto.SignupResponse.from(info);
        return ApiResponse.success(response);
    }
}


