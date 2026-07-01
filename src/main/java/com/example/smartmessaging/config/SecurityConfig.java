package com.example.smartmessaging.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. 개발 편의를 위해 CSRF 방어 비활성화
            .csrf(AbstractHttpConfigurer::disable)
            
            // 2. 개발 초기이므로 모든 URL 요청에 대해 인증 없이 접근 허용 (permitAll)
            .authorizeHttpRequests(authorize -> authorize
                .anyRequest().permitAll()
            )
            
            // 3. 스프링 시큐리티 기본 제공 로그인창 및 로그아웃 기능 일시 비활성화 (추후 로그인 화면 개발 시 활성화)
            .formLogin(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // 회원 비밀번호 암호화에 필수적인 BCryptPasswordEncoder 빈 등록
        return new BCryptPasswordEncoder();
    }
}
