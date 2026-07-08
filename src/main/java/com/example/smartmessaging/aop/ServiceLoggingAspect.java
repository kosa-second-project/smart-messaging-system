package com.example.smartmessaging.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;

@Slf4j
@Aspect
@Component
public class ServiceLoggingAspect {

    // com.example.smartmessaging.service 패키지 하위의 모든 클래스 및 메소드를 포인트컷으로 지정
    @Pointcut("within(com.example.smartmessaging.service..*)")
    public void serviceMethods() {}

    /**
     * 모든 비즈니스 서비스 로직 실행 전후에 개입하여 실행 시간 측정 및 입출력 로그를 남깁니다.
     */
    @Around("serviceMethods()")
    public Object logServiceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        // 1. 서비스 로직 시작 로그 출력 (개인정보 보호를 위해 마스킹 처리된 인자값 출력)
        log.info("[Service Start] {}.{}() | Arguments: {}", className, methodName, maskArguments(args));
        
        long start = System.currentTimeMillis();
        try {
            // 실제 서비스 메소드 실행
            Object result = joinPoint.proceed();
            
            // 2. 서비스 로직 성공 종료 및 실행 소요 시간(ms) 출력
            long executionTime = System.currentTimeMillis() - start;
            log.info("[Service End] {}.{}() | 소요 시간: {}ms", className, methodName, executionTime);
            return result;
            
        } catch (Throwable e) {
            // 3. 서비스 실행 중 예외(Exception) 발생 시 로깅 및 다시 던지기
            long executionTime = System.currentTimeMillis() - start;
            log.error("[Service Error] {}.{}() | 소요 시간: {}ms | 에러 메시지: {}", 
                    className, methodName, executionTime, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 로그 파일 및 콘솔에 고객 개인정보(이름, 이메일, 전화번호) 및 비밀번호가 평문 노출되는 것을 방어하는 필터링 유틸
     */
    private String maskArguments(Object[] args) {
        if (args == null) return "[]";
        
        java.util.List<String> maskedList = new java.util.ArrayList<>();
        for (Object arg : args) {
            if (arg == null) {
                maskedList.add("null");
                continue;
            }
            
            String argClass = arg.getClass().getSimpleName();
            
            // 고객 정보(CustomerVO), 사원 정보(UsersVO) 등 민감한 개인정보를 품은 VO 객체들은 필드값을 노출하지 않고 마스킹 처리
            if (argClass.startsWith("Customer") || argClass.startsWith("Users") || argClass.startsWith("SendTarget")) {
                maskedList.add(argClass + "{PROTECTED_PERSONAL_DATA}");
            } else if (arg instanceof Collection<?> collection) {
                maskedList.add(argClass + "(size=" + collection.size() + ")");
            } else if (arg instanceof Map<?, ?> map) {
                maskedList.add(argClass + "(size=" + map.size() + ")");
            } else if (arg.getClass().isArray()) {
                maskedList.add(argClass + "(length=" + java.lang.reflect.Array.getLength(arg) + ")");
            } else {
                String strVal = arg.toString();
                // 패스워드나 크리덴셜 관련 텍스트가 인자값에 보일 경우 강제 차단
                if (strVal.toLowerCase().contains("pwd") || strVal.toLowerCase().contains("password")) {
                    maskedList.add("[PROTECTED_CREDENTIALS]");
                } else {
                    maskedList.add(strVal);
                }
            }
        }
        return maskedList.toString();
    }
}
