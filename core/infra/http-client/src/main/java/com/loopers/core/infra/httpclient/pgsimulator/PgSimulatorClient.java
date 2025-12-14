package com.loopers.core.infra.httpclient.pgsimulator;

import com.loopers.core.infra.httpclient.config.PgSimulatorFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import static com.loopers.core.infra.httpclient.pgsimulator.dto.PgSimulatorV1Dto.PgSimulatorPaymentRequest;
import static com.loopers.core.infra.httpclient.pgsimulator.dto.PgSimulatorV1Dto.PgSimulatorPaymentResponse;

@FeignClient(
        name = "pgSimulatorClient",
        url = "${http-client.pg-simulator.url}",
        path = "${http-client.pg-simulator.path}",
        configuration = PgSimulatorFeignConfig.class
)
public interface PgSimulatorClient {

    @PostMapping
    ResponseEntity<PgSimulatorPaymentResponse> pay(
            @RequestHeader("X-USER-ID") String userId,
            @RequestBody PgSimulatorPaymentRequest request
    );
}
