package com.loopers.interfaces.api.user;
import com.loopers.domain.user.User;
import org.springframework.stereotype.Component;

@Component
public class UserDtoMapper {
    public User toEntity(UserRequestDto dto) {
        return User.builder()
                .id(dto.id())
                .email(dto.email())
                .birthDate(dto.birthDate())
                .gender(dto.gender())
                .build();
    }

    public UserResponseDto toResponse(User user) {
        return new UserResponseDto(
                user.getId(),
                user.getEmail(),
                user.getBirthDate(),
                user.getGender()
        );
    }
}
