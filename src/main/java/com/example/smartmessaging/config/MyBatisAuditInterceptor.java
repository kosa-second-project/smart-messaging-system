package com.example.smartmessaging.config;

import com.example.smartmessaging.dto.vo.BaseVO;
import com.example.smartmessaging.security.CustomUserDetails;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Properties;

@Slf4j
@Component
@Intercepts({
    @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})
})
public class MyBatisAuditInterceptor implements Interceptor {

    /**
     * Populates audit fields on MyBatis update parameters that extend {@code BaseVO}.
     *
     * @param invocation the MyBatis invocation to continue
     * @return the result of proceeding with the invocation
     * @throws Throwable if the intercepted MyBatis call fails
     */
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
        Object parameter = invocation.getArgs()[1];
        SqlCommandType sqlCommandType = mappedStatement.getSqlCommandType();

        // 1. 전송 파라미터가 BaseVO를 상속받은 객체인지 검사 (공통 감사 필드 대상)
        if (parameter instanceof BaseVO) {
            BaseVO baseVO = (BaseVO) parameter;

            // 2. 스프링 시큐리티 세션에서 로그인한 사용자의 PK ID(userId) 추출
            Long currentUserId = null;
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
                CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
                currentUserId = userDetails.getUserId();
            }

            // (비인증 상태의 회원가입이나 초기화 배치 시의 안전장치로 기본 시스템 어드민 ID 1L 매핑)
            if (currentUserId == null) {
                currentUserId = 1L; 
            }

            // 3. SQL 실행 유형(INSERT, UPDATE)에 따라 Audit 필드 값 주입 (시간 및 삭제 여부 기본값은 DB가 자동 처리하므로 생략)
            if (SqlCommandType.INSERT.equals(sqlCommandType)) {
                if (baseVO.getCreatedBy() == null) baseVO.setCreatedBy(currentUserId);
                if (baseVO.getUpdatedBy() == null) baseVO.setUpdatedBy(currentUserId);
                log.debug("[MyBatis Audit] INSERT 생성자/수정자 자동 주입 완료 - UserID: {}", currentUserId);
            } else if (SqlCommandType.UPDATE.equals(sqlCommandType)) {

                if (baseVO.getUpdatedBy() == null) baseVO.setUpdatedBy(currentUserId);
                log.debug("[MyBatis Audit] UPDATE 수정자 자동 주입 완료 - UserID: {}", currentUserId);
            }

        }

        return invocation.proceed();
    }

    /**
     * Wraps the target object with this interceptor.
     *
     * @param target the object to wrap
     * @return the wrapped object
     */
    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    /**
     * Ignores interceptor configuration properties.
     *
     * @param properties interceptor properties
     */
    @Override
    public void setProperties(Properties properties) {}
}
