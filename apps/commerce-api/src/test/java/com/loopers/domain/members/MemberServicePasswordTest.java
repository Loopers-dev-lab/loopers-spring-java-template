package com.loopers.domain.members;

import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MemberServicePasswordTest {

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("회원 가입 시 비밀번호가 암호화되어 저장된다")
    @Test
    void shouldEncryptPassword_whenRegisterMember() {
        String plainPassword = "password123";

        memberService.registerMember("test123", "홍길동", "test@example.com", plainPassword, "1990-01-01", "MALE");

        MemberModel savedMember = memberRepository.findByMemberId("test123").orElseThrow();

        assertThat(savedMember.getPassword()).isNotEqualTo(plainPassword);
        assertThat(savedMember.getPassword()).startsWith("$2a$");
        assertThat(passwordEncoder.matches(plainPassword, savedMember.getPassword())).isTrue();
    }

    @DisplayName("같은 비밀번호라도 매번 다른 해시값이 생성된다")
    @Test
    void shouldGenerateDifferentHash_forSamePassword() {
        String plainPassword = "password123";

        memberService.registerMember("test1", "홍길동1", "test1@example.com", plainPassword, "1990-01-01", "MALE");
        memberService.registerMember("test2", "홍길동2", "test2@example.com", plainPassword, "1990-01-01", "MALE");

        MemberModel member1 = memberRepository.findByMemberId("test1").orElseThrow();
        MemberModel member2 = memberRepository.findByMemberId("test2").orElseThrow();

        assertThat(member1.getPassword()).isNotEqualTo(member2.getPassword());
        assertThat(passwordEncoder.matches(plainPassword, member1.getPassword())).isTrue();
        assertThat(passwordEncoder.matches(plainPassword, member2.getPassword())).isTrue();
    }
}
