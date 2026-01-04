package com.loopers.utils;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Table;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
public class DatabaseCleanUp implements InitializingBean {

    @PersistenceContext
    private EntityManager entityManager;

    private final List<String> tableNames = new ArrayList<>();

    @Override
    public void afterPropertiesSet() {
        entityManager.getMetamodel().getEntities().stream()
            .filter(entity -> entity.getJavaType().getAnnotation(Entity.class) != null)
            .map(entity -> entity.getJavaType().getAnnotation(Table.class).name())
            .forEach(tableNames::add);
    }

    @Transactional
    public void truncateAllTables() {
        entityManager.flush();
        entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate();

        for (String table : tableNames) {
            String tableName = table;
            // 테이블 이름에 이미 백틱이 있으면 생략
            if (!tableName.startsWith("`") && !tableName.endsWith("`")) {
                tableName = "`" + tableName + "`";
            }
            
            // 테이블이 존재하는지 확인 후 TRUNCATE 수행
            try {
                // 테이블 존재 여부 확인
                String checkTableSql = "SELECT COUNT(*) FROM information_schema.tables " +
                                     "WHERE table_schema = DATABASE() AND table_name = ?";
                Long count = ((Number) entityManager.createNativeQuery(checkTableSql)
                    .setParameter(1, table.replace("`", ""))
                    .getSingleResult()).longValue();
                
                if (count > 0) {
                    entityManager.createNativeQuery("TRUNCATE TABLE " + tableName).executeUpdate();
                }
            } catch (Exception e) {
                // 테이블이 없거나 오류가 발생하면 무시하고 계속 진행
                // 로그는 남기지 않음 (테스트 환경에서 정상적인 상황일 수 있음)
            }
        }

        entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();
    }
}
