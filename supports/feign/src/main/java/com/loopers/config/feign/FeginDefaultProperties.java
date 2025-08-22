package com.loopers.config.feign;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@Configuration
@ConfigurationProperties(prefix = "feign.client.config.default")
public class FeginDefaultProperties {
    @JsonProperty("connect-timeout")
    private int connectTimeout;

    @JsonProperty("read-timeout")
    private int readTimeout;
}
