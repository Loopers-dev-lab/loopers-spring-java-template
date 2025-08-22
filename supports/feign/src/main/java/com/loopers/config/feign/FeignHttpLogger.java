package com.loopers.config.feign;


import feign.Logger;
import feign.Request;
import feign.Response;
import feign.Util;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Map;

import static feign.Util.*;

@Slf4j
public class FeignHttpLogger extends Logger {

    private static final String SERVICE_NAME = "PaymentService";
    private static final String REQUEST_LOG_KEY = SERVICE_NAME + "   ---->   HTTP";
    private static final String RESPONSE_LOG_KEY = SERVICE_NAME + "   <----   HTTP";
    private static final String HTTP_VERSION = "HTTP/1.1";
    private static final String BINARY_DATA_PLACEHOLDER = "Binary data";
    private static final String END_HTTP_FORMAT = " END HTTP (%s-byte body)";
    private static final Charset DEFAULT_CHARSET = UTF_8;

    @Override
    protected void log(String configKey, String format, Object... args) {
        String logMessage = String.format(methodLocalTag(configKey).concat(format), args);
        log.info(logMessage);
    }

    protected static String methodLocalTag(String configKey) {
        return String.format("[%s] ", configKey);
    }

    @Override
    protected Response logAndRebufferResponse(String configKey,
                                              Level logLevel,
                                              Response response,
                                              long elapsedTime) throws IOException {
        if (shouldNotLog(logLevel)) {
            return response;
        }

        StringBuilder logMessage = buildResponseLogMessage(response, logLevel, elapsedTime);

        if (logLevel.ordinal() >= Level.HEADERS.ordinal()) {
            appendHeaders(logMessage, response.headers());
            Response rebuiltResponse = handleResponseBody(response, logLevel, logMessage);
            log(RESPONSE_LOG_KEY, logMessage.toString());
            return rebuiltResponse;
        } else {
            // BASIC 레벨에서도 로그는 찍어야 함
            logMessage.append(String.format(END_HTTP_FORMAT, 0));
            log(RESPONSE_LOG_KEY, logMessage.toString());
            return response;
        }
    }

    @Override
    protected void logRequest(String configKey, Level logLevel, Request request) {
        if (shouldNotLog(logLevel)) {
            return;
        }

        StringBuilder logMessage = buildRequestLogMessage(request);

        if (logLevel.ordinal() >= Level.HEADERS.ordinal()) {
            appendHeaders(logMessage, request.headers());
            appendRequestBody(request, logLevel, logMessage);
        } else {
            // BASIC 레벨에서도 END HTTP 로그는 필요
            logMessage.append(String.format(END_HTTP_FORMAT, 0));
        }

        log(REQUEST_LOG_KEY, logMessage.toString());
    }

    private boolean shouldNotLog(Level logLevel) {
        return logLevel.compareTo(Level.NONE) <= 0;
    }

    private StringBuilder buildResponseLogMessage(Response response, Level logLevel, long elapsedTime) {
        StringBuilder logMessage = new StringBuilder();
        String reason = formatReason(response.reason(), logLevel);
        logMessage.append(String.format(" %s %s%s (%sms)",
                HTTP_VERSION, response.status(), reason, elapsedTime));
        return logMessage;
    }

    private StringBuilder buildRequestLogMessage(Request request) {
        StringBuilder logMessage = new StringBuilder();
        logMessage.append(String.format(" %s %s %s",
                request.httpMethod().name(), request.url(), HTTP_VERSION));
        return logMessage;
    }

    private String formatReason(String reason, Level logLevel) {
        return reason != null && logLevel.compareTo(Level.NONE) > 0
                ? " " + reason
                : "";
    }

    private void appendHeaders(StringBuilder logMessage, Map<String, Collection<String>> headers) {
        headers.forEach((field, values) ->
                valuesOrEmpty(headers, field).forEach(value ->
                        logMessage.append(String.format("\n%s: %s", field, value))
                )
        );
    }

    private Response handleResponseBody(Response response, Level logLevel, StringBuilder logMessage)
            throws IOException {
        // body가 null이거나 No Content 상태인 경우
        if (response.body() == null || isNoContentStatus(response.status())) {
            logMessage.append(String.format(END_HTTP_FORMAT, 0));
            return response;
        }

        // body를 읽어서 버퍼링 (에러 응답도 포함)
        byte[] bodyData = Util.toByteArray(response.body().asInputStream());
        int bodyLength = bodyData.length;

        // FULL 레벨이고 body가 있으면 내용 로깅
        if (shouldLogFullBody(logLevel, bodyLength)) {
            appendBodyContent(logMessage, bodyData);
        }

        logMessage.append(String.format(END_HTTP_FORMAT, bodyLength));

        // 중요: body를 다시 설정하여 반환 (에러든 성공이든 상관없이)
        return response.toBuilder().body(bodyData).build();
    }

    private void appendRequestBody(Request request, Level logLevel, StringBuilder logMessage) {
        int bodyLength = 0;

        if (request.body() != null) {
            bodyLength = request.length();

            if (logLevel.ordinal() >= Level.FULL.ordinal()) {
                String bodyText = extractRequestBodyText(request);
                logMessage.append("\n")
                        .append(bodyText != null ? bodyText : BINARY_DATA_PLACEHOLDER);
            }
        }

        logMessage.append(String.format(END_HTTP_FORMAT, bodyLength));
    }

    private void appendBodyContent(StringBuilder logMessage, byte[] bodyData) {
        logMessage.append("\n");
        String body = decodeOrDefault(bodyData, DEFAULT_CHARSET, BINARY_DATA_PLACEHOLDER);
        logMessage.append(StringEscapeUtils.unescapeJava(body));
    }

    private String extractRequestBodyText(Request request) {
        return request.charset() != null
                ? new String(request.body(), request.charset())
                : null;
    }

    private boolean isNoContentStatus(int status) {
        return status == 204 || status == 205;
    }

    private boolean shouldLogFullBody(Level logLevel, int bodyLength) {
        return logLevel.ordinal() >= Level.FULL.ordinal() && bodyLength > 0;
    }
}
