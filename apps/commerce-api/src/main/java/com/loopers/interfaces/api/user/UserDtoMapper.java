package com.loopers.interfaces.api.user;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import org.springframework.stereotype.Component;

@Component
public class UserDtoMapper {
    public User toEntity(UserRequestDto dto) {
        return User.builder()
                .userId(dto.id())
                .email(dto.email())
                .birthDate(dto.birthDate())
                .gender(Gender.from(dto.gender()))
                .build();
    }

    public UserResponseDto toResponse(User user) {
        return new UserResponseDto(
                user.getUserId(),
                user.getEmail(),
                user.getBirthDate(),
                user.getGender().name()
        );
    }
}
