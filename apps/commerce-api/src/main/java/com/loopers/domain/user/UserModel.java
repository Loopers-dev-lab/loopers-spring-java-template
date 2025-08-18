package com.loopers.domain.user;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.user.embeded.Birth;
import com.loopers.domain.user.embeded.Grender;
import com.loopers.domain.user.embeded.UserEmail;
import com.loopers.domain.user.embeded.UserLoginId;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.ToString;

@Entity
@Table(name = "member",
    indexes = {
        @Index(name = "idx_member_login_id", columnList = "login_id"),
        @Index(name = "idx_member_email", columnList = "user_email")
    })
@ToString
public class UserModel extends BaseEntity {
    @Embedded
    private UserLoginId loginId;
    @Embedded
    private UserEmail email;
    @Embedded
    private Birth birth;
    @Embedded
    private Grender grender;

    public UserModel() {
    }
    private UserModel(UserLoginId loginId, UserEmail email, Birth birth, Grender grender) {
        this.loginId = loginId;
        this.email = email;
        this.birth = birth;
        this.grender = grender;
    }
    public static UserModel register(String loginId, String email, String birth, String grender) {
        return new UserModel(
                new UserLoginId(loginId),
                new UserEmail(email),
                new Birth(birth),
                new Grender(grender)
        );

    }

    public String getLoginId() {
        return loginId.getLoginId();
    }

    public String getEmail() {
        return email.getUserEmail();
    }

    public String getBirth() {
        return birth.getBirth().toString();
    }

    public String getGrender() {
        return grender.getGrender();
    }
}
