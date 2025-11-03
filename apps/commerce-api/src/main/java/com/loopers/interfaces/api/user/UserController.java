package com.loopers.interfaces.api.user;

import com.loopers.domain.point.PointService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User", description = "유저 관련 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final UserDtoMapper userDtoMapper;
    private final PointService pointService;
    @Operation(summary = "회원 가입")
    @PostMapping("/users")
    public ApiResponse<UserResponseDto> signUp(
            @RequestBody UserRequestDto userRequestDto){
        User user = userDtoMapper.toEntity(userRequestDto);
        User saved = userService.saveUser(user);
        pointService.create(saved.getId());
        return ApiResponse.success(userDtoMapper.toResponse(saved));
    }

    @Operation(summary = "내 정보 조회")
    @GetMapping("/users/me")
    public ResponseEntity<ApiResponse<Object>> getUser(
            @RequestParam("id") String id
    ) {
        User user = userService.getUser(id);

        if (user == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.fail("404","존재하지 않는 ID입니다."));
        }

        return ResponseEntity.ok(
                ApiResponse.success(userDtoMapper.toResponse(user))
        );
    }

}
