package com.loopers.core.infra.database.mysql.user.entity;

import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.common.vo.DeletedAt;
import com.loopers.core.domain.common.vo.UpdatedAt;
import com.loopers.core.domain.user.User;
import com.loopers.core.domain.user.type.UserGender;
import com.loopers.core.domain.user.vo.UserBirthDay;
import com.loopers.core.domain.user.vo.UserEmail;
import com.loopers.core.domain.user.vo.UserId;
import com.loopers.core.domain.user.vo.UserIdentifier;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_user_identifier", columnList = "identifier"),
                @Index(name = "idx_user_email", columnList = "email")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String identifier;

    private String email;

    private LocalDate birthDay;

    private String gender;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    public static UserEntity from(User user) {
        return UserEntity.builder()
                .id(
                        Optional.ofNullable(user.getUserId().value())
                                .map(Long::parseLong)
                                .orElse(null)
                )
                .identifier(user.getIdentifier().value())
                .email(user.getEmail().value())
                .birthDay(user.getBirthDay().value())
                .gender(user.getGender().name())
                .createdAt(user.getCreatedAt().value())
                .updatedAt(user.getUpdatedAt().value())
                .deletedAt(user.getDeletedAt().value())
                .build();
    }

    public User to() {
        return User.mappedBy(
                new UserId(this.id.toString()),
                new UserIdentifier(this.identifier),
                new UserEmail(this.email),
                new UserBirthDay(this.birthDay),
                UserGender.valueOf(this.gender),
                new CreatedAt(this.createdAt),
                new UpdatedAt(this.updatedAt),
                new DeletedAt(this.deletedAt)
        );
    }
}
