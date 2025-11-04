package com.loopers.infrastructure.user;

import com.loopers.domain.user.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserJpaRepository extends JpaRepository<User, Long> {
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.loginId.id = :loginId")
    boolean existsByLoginId(@Param("loginId") String loginId);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.email.address = :email")
    boolean existsByEmail(@Param("email") String email);

    @Query("SELECT u FROM User u WHERE u.loginId.id = :loginId")
    Optional<User> findByLoginId(@Param("loginId") String loginId);
}
