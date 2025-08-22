package com.loopers.config.feign;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Feign 클라이언트 테스트를 위한 설정
 * WireMock을 사용한 외부 서비스 Mock 서버 설정
 */
@TestConfiguration
public class FeignTestConfig {

    /**
     * WireMock 서버 설정
     * 테스트에서 외부 API 호출을 모킹하기 위한 서버
     */
    @Bean
    public WireMockServer wireMockServer() {
        WireMockServer wireMockServer = new WireMockServer(
            WireMockConfiguration.options()
                .port(8082) // PG 시뮬레이터와 같은 포트 사용
                .usingFilesUnderClasspath("wiremock")
        );
        
        return wireMockServer;
    }

    /**
     * 테스트용 ObjectMapper
     */
    @Bean
    @Primary
    public ObjectMapper testObjectMapper() {
        return new ObjectMapper()
            .findAndRegisterModules(); // JSR310 모듈 등 자동 등록
    }
}