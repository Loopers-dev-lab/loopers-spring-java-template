package com.loopers.interfaces.api.members;

import com.loopers.application.members.MemberFacade;
import com.loopers.domain.members.enums.Gender;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MemberPasswordSecurityTest {

    private final TestRestTemplate testRestTemplate;
    private final MemberFacade memberFacade;
    private final DatabaseCleanUp databaseCleanUp;

    @Autowired
    public MemberPasswordSecurityTest(
            TestRestTemplate testRestTemplate,
            MemberFacade memberFacade,
            DatabaseCleanUp databaseCleanUp
    ) {
        this.testRestTemplate = testRestTemplate;
        this.memberFacade = memberFacade;
        this.databaseCleanUp = databaseCleanUp;
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("회원 가입 응답에 비밀번호가 포함되지 않는다")
    @Test
    void shouldNotIncludePassword_inRegistrationResponse() {
        MemberV1Dto.RegisterMemberRequest request = new MemberV1Dto.RegisterMemberRequest(
                "test123", "test@example.com", "secretPassword123", "1990-01-01", Gender.MALE
        );

        ResponseEntity<String> response = testRestTemplate.exchange(
                "/api/v1/members",
                HttpMethod.POST,
                new HttpEntity<>(request),
                String.class
        );

        String responseBody = response.getBody();

        assertThat(responseBody).doesNotContain("password");
        assertThat(responseBody).doesNotContain("secretPassword123");

        assertThat(responseBody).contains("test123");
        assertThat(responseBody).contains("test@example.com");
    }

    @DisplayName("내 정보 조회 응답에 비밀번호가 포함되지 않는다")
    @Test
    void shouldNotIncludePassword_inMemberInfoResponse() {
        memberFacade.registerMember("test123", "test@example.com", "secretPassword123", "1990-01-01", Gender.MALE);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-USER-ID", "test123");

        ResponseEntity<String> response = testRestTemplate.exchange(
                "/api/v1/members/me",
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                String.class
        );

        String responseBody = response.getBody();

        assertThat(responseBody).doesNotContain("password");
        assertThat(responseBody).doesNotContain("secretPassword123");

        assertThat(responseBody).contains("test123");
        assertThat(responseBody).contains("test@example.com");
    }
}
