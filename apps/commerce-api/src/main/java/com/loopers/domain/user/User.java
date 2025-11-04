package com.loopers.domain.user;

import com.loopers.application.user.UserCommand;
import com.loopers.domain.BaseEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;

@Entity
@Table(name = "users")
@Getter
public class User extends BaseEntity {

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "id", column = @Column(name = "login_id", nullable = false, unique = true))
    })
    private LoginId loginId;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "address", column = @Column(name = "email", nullable = false, unique = true))
    })
    private Email email;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "date", column = @Column(name = "birth_date", nullable = false))
    })
    private BirthDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Builder
    private User(String loginId, String email, String birthDate, String gender) {
        this.loginId = new LoginId(loginId);
        this.email = new Email(email);
        this.birthDate = new BirthDate(birthDate);
        this.gender = Gender.fromCode(gender);
    }

    public User() {
    }

    public static User createUser(UserCommand.SignupCommand signupCommand) {
        return User.builder()
                .loginId(signupCommand.loginId())
                .email(signupCommand.email())
                .birthDate(signupCommand.birthDate())
                .gender(signupCommand.gender())
                .build();
    }
}
