package com.loopers.core.infra.httpclient.config.decoder;

import com.loopers.core.domain.error.HttpClientException;
import feign.Response;
import feign.Util;
import feign.codec.ErrorDecoder;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Objects;

public class CustomFeignDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        try {
            String message = "";

            if (Objects.nonNull(response.body())) {
                message = Util.toString(response.body().asReader(Charset.defaultCharset()));
            }

            return HttpClientException.of(response.status(), message);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
