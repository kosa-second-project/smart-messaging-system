package com.example.smartmessaging.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Arrays;

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

        // 1. 서비스 로직 시작 로그 출력
        log.info("[Service Start] {}.{}() | Arguments: {}", className, methodName, Arrays.toString(args));
        
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
}
