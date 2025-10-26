package com.loopers.interfaces.api.user;

import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/examples")
public class UserV1Controller implements UserV1ApiSpec {

    private final UserFacade userFacade;

    @GetMapping("/{exampleId}")
    @Override
    public ApiResponse<UserV1Dto.ExampleResponse> getExample(
        @PathVariable(value = "exampleId") Long exampleId
    ) {
        UserInfo info = userFacade.getExample(exampleId);
        UserV1Dto.ExampleResponse response = UserV1Dto.ExampleResponse.from(info);
        return ApiResponse.success(response);
    }
}
