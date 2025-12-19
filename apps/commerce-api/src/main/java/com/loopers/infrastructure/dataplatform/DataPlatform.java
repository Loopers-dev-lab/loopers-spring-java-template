package com.loopers.infrastructure.dataplatform;

import org.springframework.stereotype.Component;

/**
 * 데이터 플랫폼 외부 API Mock 클래스.
 * 외부 API를 호출하며, 유실될 수 있다고 가정한다.
 */
@Component
public class DataPlatform {

    public boolean send(String data) {
        // 외부 API 호출을 시뮬레이션
        return true;
    }
}
