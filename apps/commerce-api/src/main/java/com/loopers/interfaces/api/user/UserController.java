package com.loopers.interfaces.api.user;

import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User", description = "유저 관련 API")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final UserDtoMapper userDtoMapper;

    @Operation(summary = "유저 회원 가입")
    @PostMapping("signUp")
    public ApiResponse<UserResponseDto> signUp(
            @RequestBody UserRequestDto userRequestDto){
        User user = userDtoMapper.toEntity(userRequestDto);
        User saved = userService.saveUser(user);
        return ApiResponse.success(userDtoMapper.toResponse(saved));
    }
}
