package com.loopers.interfaces.api;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 전역 API 예외 처리 핸들러.
 * <p>
 * 애플리케이션 전역에서 발생하는 예외를 가로채어
 * 일관된 형식의 에러 응답을 생성합니다.
 * </p>
 *
 * <h3>처리하는 예외 유형</h3>
 * <ul>
 *   <li>CoreException: 도메인 비즈니스 로직 예외</li>
 *   <li>Validation 예외: 요청 데이터 검증 실패</li>
 *   <li>HTTP 메시지 변환 예외: JSON 파싱 오류</li>
 *   <li>기타 예상치 못한 예외</li>
 * </ul>
 *
 * @author Loopers
 * @version 1.0
 */
@RestControllerAdvice
@Slf4j
public class ApiControllerAdvice {
    /**
     * CoreException을 처리합니다.
     *
     * @param e 발생한 CoreException
     * @return 에러 응답
     */
    @ExceptionHandler
    public ResponseEntity<ApiResponse<?>> handle(CoreException e) {
        log.warn("CoreException : {}", e.getCustomMessage() != null ? e.getCustomMessage() : e.getMessage(), e);
        return failureResponse(e.getErrorType(), e.getCustomMessage());
    }

    /**
     * 요청 파라미터 타입 불일치 예외를 처리합니다.
     *
     * @param e 발생한 MethodArgumentTypeMismatchException
     * @return BAD_REQUEST 에러 응답
     */
    @ExceptionHandler
    public ResponseEntity<ApiResponse<?>> handleBadRequest(MethodArgumentTypeMismatchException e) {
        String name = e.getName();
        String type = e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "unknown";
        String value = e.getValue() != null ? e.getValue().toString() : "null";
        String message = String.format("요청 파라미터 '%s' (타입: %s)의 값 '%s'이(가) 잘못되었습니다.", name, type, value);
        return failureResponse(ErrorType.BAD_REQUEST, message);
    }

    /**
     * 필수 요청 파라미터 누락 예외를 처리합니다.
     *
     * @param e 발생한 MissingServletRequestParameterException
     * @return BAD_REQUEST 에러 응답
     */
    @ExceptionHandler
    public ResponseEntity<ApiResponse<?>> handleBadRequest(MissingServletRequestParameterException e) {
        String name = e.getParameterName();
        String type = e.getParameterType();
        String message = String.format("필수 요청 파라미터 '%s' (타입: %s)가 누락되었습니다.", name, type);
        return failureResponse(ErrorType.BAD_REQUEST, message);
    }

    /**
     * 필수 요청 헤더 누락 예외를 처리합니다.
     *
     * @param e 발생한 MissingRequestHeaderException
     * @return BAD_REQUEST 에러 응답
     */
    @ExceptionHandler
    public ResponseEntity<ApiResponse<?>> handleBadRequest(MissingRequestHeaderException e) {
        String name = e.getHeaderName();
        String message = String.format("필수 요청 헤더 '%s'가 누락되었습니다.", name);
        return failureResponse(ErrorType.BAD_REQUEST, message);
    }

    /**
     * 요청 데이터 유효성 검증 실패 예외를 처리합니다.
     *
     * @param e 발생한 MethodArgumentNotValidException
     * @return BAD_REQUEST 에러 응답 (검증 실패 필드 정보 포함)
     */
    @ExceptionHandler
    public ResponseEntity<ApiResponse<?>> handleBadRequest(MethodArgumentNotValidException e) {
        String message = Stream.concat(
                        e.getBindingResult().getFieldErrors().stream()
                                .map(err -> String.format("필드 '%s' %s", err.getField(), err.getDefaultMessage())),
                        e.getBindingResult().getGlobalErrors().stream()
                                .map(err -> String.format("객체 '%s' %s", err.getObjectName(), err.getDefaultMessage()))
                )
                .filter(str -> str != null && !str.isBlank())
                .collect(Collectors.joining(", "));
        return failureResponse(ErrorType.BAD_REQUEST, message.isBlank() ? null : message);
    }

    /**
     * HTTP 메시지 읽기 실패 예외를 처리합니다.
     * JSON 파싱 오류, 타입 불일치 등을 처리합니다.
     *
     * @param e 발생한 HttpMessageNotReadableException
     * @return BAD_REQUEST 에러 응답
     */
    @ExceptionHandler
    public ResponseEntity<ApiResponse<?>> handleBadRequest(HttpMessageNotReadableException e) {
        String errorMessage;
        Throwable rootCause = e.getRootCause();

        if (rootCause instanceof InvalidFormatException invalidFormat) {
            String fieldName = invalidFormat.getPath().stream()
                    .map(ref -> ref.getFieldName() != null ? ref.getFieldName() : "?")
                    .collect(Collectors.joining("."));

            String valueIndicationMessage = "";
            if (invalidFormat.getTargetType().isEnum()) {
                Class<?> enumClass = invalidFormat.getTargetType();
                String enumValues = Arrays.stream(enumClass.getEnumConstants())
                        .map(Object::toString)
                        .collect(Collectors.joining(", "));
                valueIndicationMessage = "사용 가능한 값 : [" + enumValues + "]";
            }

            String expectedType = invalidFormat.getTargetType().getSimpleName();
            Object value = invalidFormat.getValue();

            errorMessage = String.format("필드 '%s'의 값 '%s'이(가) 예상 타입(%s)과 일치하지 않습니다. %s",
                    fieldName, value, expectedType, valueIndicationMessage);

        } else if (rootCause instanceof MismatchedInputException mismatchedInput) {
            String fieldPath = mismatchedInput.getPath().stream()
                    .map(ref -> ref.getFieldName() != null ? ref.getFieldName() : "?")
                    .collect(Collectors.joining("."));
            errorMessage = String.format("필수 필드 '%s'이(가) 누락되었습니다.", fieldPath);

        } else if (rootCause instanceof JsonMappingException jsonMapping) {
            String fieldPath = jsonMapping.getPath().stream()
                    .map(ref -> ref.getFieldName() != null ? ref.getFieldName() : "?")
                    .collect(Collectors.joining("."));
            errorMessage = String.format("필드 '%s'에서 JSON 매핑 오류가 발생했습니다: %s",
                    fieldPath, jsonMapping.getOriginalMessage());

        } else {
            errorMessage = "요청 본문을 처리하는 중 오류가 발생했습니다. JSON 메세지 규격을 확인해주세요.";
        }

        return failureResponse(ErrorType.BAD_REQUEST, errorMessage);
    }

    /**
     * 서버 웹 입력 예외를 처리합니다.
     *
     * @param e 발생한 ServerWebInputException
     * @return BAD_REQUEST 에러 응답
     */
    @ExceptionHandler
    public ResponseEntity<ApiResponse<?>> handleBadRequest(ServerWebInputException e) {
        String missingParams = extractMissingParameter(e.getReason() != null ? e.getReason() : "");
        if (!missingParams.isEmpty()) {
            String message = String.format("필수 요청 값 '%s'가 누락되었습니다.", missingParams);
            return failureResponse(ErrorType.BAD_REQUEST, message);
        } else {
            return failureResponse(ErrorType.BAD_REQUEST, null);
        }
    }

    /**
     * 리소스를 찾을 수 없는 예외를 처리합니다.
     *
     * @param e 발생한 NoResourceFoundException
     * @return NOT_FOUND 에러 응답
     */
    @ExceptionHandler
    public ResponseEntity<ApiResponse<?>> handleNotFound(NoResourceFoundException e) {
        return failureResponse(ErrorType.NOT_FOUND, null);
    }

    /**
     * 예상치 못한 모든 예외를 처리합니다.
     *
     * @param e 발생한 Throwable
     * @return INTERNAL_ERROR 에러 응답
     */
    @ExceptionHandler
    public ResponseEntity<ApiResponse<?>> handle(Throwable e) {
        log.error("Exception : {}", e.getMessage(), e);
        return failureResponse(ErrorType.INTERNAL_ERROR, null);
    }

    /**
     * 에러 메시지에서 누락된 파라미터명을 추출합니다.
     *
     * @param message 에러 메시지
     * @return 추출된 파라미터명
     */
    private String extractMissingParameter(String message) {
        Pattern pattern = Pattern.compile("'(.+?)'");
        Matcher matcher = pattern.matcher(message);
        return matcher.find() ? matcher.group(1) : "";
    }

    /**
     * 에러 타입과 메시지를 기반으로 실패 응답을 생성합니다.
     *
     * @param errorType 에러 타입
     * @param errorMessage 에러 메시지
     * @return 에러 응답
     */
    private ResponseEntity<ApiResponse<?>> failureResponse(ErrorType errorType, String errorMessage) {
        return ResponseEntity.status(errorType.getStatus())
                .body(ApiResponse.fail(errorType.getCode(), errorMessage != null ? errorMessage : errorType.getMessage()));
    }
}
