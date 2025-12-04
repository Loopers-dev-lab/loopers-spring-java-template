package com.loopers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public final class JacksonUtil implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    public JacksonUtil() {
    }

    public static ObjectMapper getObjectMapper() {
        return applicationContext.getBean(ObjectMapper.class);
    }

    public static String convertToString(Object object) {
        try {
            return getObjectMapper().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("객체를 JSON 문자열로 변환하는 중 오류가 발생했습니다", e);
        }
    }

    public static <T> T convertToObject(String json, Class<T> clazz) {
        try {
            return getObjectMapper().readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 문자열을 객체로 변환하는 중 오류가 발생했습니다", e);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        JacksonUtil.applicationContext = context;
    }
}
