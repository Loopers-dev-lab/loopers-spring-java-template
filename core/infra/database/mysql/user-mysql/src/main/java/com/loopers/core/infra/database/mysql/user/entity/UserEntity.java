package com.loopers.core.infra.database.mysql.user.entity;

import com.loopers.core.infra.database.mysql.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_user_identifier", columnList = "identifier"),
                @Index(name = "idx_user_email", columnList = "email")
        }
)
public class UserEntity extends BaseEntity {

    private String identifier;

    private String email;

    private LocalDateTime birthDay;

    private String gender;
}
