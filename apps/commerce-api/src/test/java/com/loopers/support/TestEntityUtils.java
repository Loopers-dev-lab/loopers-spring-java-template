package com.loopers.support;

import com.loopers.domain.BaseEntity;

import java.lang.reflect.Field;
import java.time.ZonedDateTime;

/**
 * 테스트에서 Entity의 final 필드들을 설정하기 위한 유틸리티 클래스
 */
public class TestEntityUtils {
    
    /**
     * Entity의 ID를 테스트용으로 설정합니다.
     * 
     * @param entity 설정할 Entity
     * @param id 설정할 ID 값
     * @param <T> BaseEntity를 상속한 Entity 타입
     * @return ID가 설정된 Entity
     */
    public static <T extends BaseEntity> T setId(T entity, Long id) {
        try {
            Field idField = getFieldFromClass(entity.getClass(), "id");
            idField.setAccessible(true);
            idField.set(entity, id);
            return entity;
        } catch (Exception e) {
            throw new IllegalStateException("테스트용 ID 설정 실패", e);
        }
    }
    
    /**
     * Entity의 생성시간을 테스트용으로 설정합니다.
     */
    public static <T extends BaseEntity> T setCreatedAt(T entity, ZonedDateTime createdAt) {
        try {
            Field createdAtField = getFieldFromClass(entity.getClass(), "createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(entity, createdAt);
            return entity;
        } catch (Exception e) {
            throw new IllegalStateException("테스트용 생성시간 설정 실패", e);
        }
    }
    
    /**
     * Entity의 ID와 생성시간을 모두 설정합니다.
     */
    public static <T extends BaseEntity> T setIdAndCreatedAt(T entity, Long id, ZonedDateTime createdAt) {
        return setCreatedAt(setId(entity, id), createdAt);
    }
    
    /**
     * Entity의 ID와 현재시간을 설정합니다.
     */
    public static <T extends BaseEntity> T setIdWithNow(T entity, Long id) {
        return setIdAndCreatedAt(entity, id, ZonedDateTime.now());
    }
    
    private static Field getFieldFromClass(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            if (clazz.getSuperclass() != null) {
                return getFieldFromClass(clazz.getSuperclass(), fieldName);
            }
            throw e;
        }
    }
}