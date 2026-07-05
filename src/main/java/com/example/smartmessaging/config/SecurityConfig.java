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
            // 1. CSRF 개발 편의를 위해 일단 비활성화
            .csrf(AbstractHttpConfigurer::disable)
            
            // 2. URL별 접근 권한 통제
            .authorizeHttpRequests(authorize -> authorize
                // 정적 리소스(CSS, JS, 이미지 등)는 로그인 없이 모두 허용
                .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                // 로그인 페이지 및 로그인 처리 주소도 누구나 접근 가능 허용
                .requestMatchers("/auth/login", "/auth/login-proc").permitAll()
                
                .requestMatchers("/api/stats/batch/**").authenticated()

                // 그 외의 모든 메뉴/요청은 인증(로그인)을 성공한 사용자만 접근 가능
                .anyRequest().authenticated()
            )
            
            // 3. 커스텀 폼 로그인 설정
            .formLogin(form -> form
                .loginPage("/auth/login")                  // 커스텀 로그인 화면 뷰 매핑 경로
                .loginProcessingUrl("/auth/login-proc")    // 로그인 폼 전송 시 처리할 필터 가로채기 주소 (POST)
                .usernameParameter("empNum")               // ID input 태그의 name 속성을 "empNum"으로 지정
                .passwordParameter("password")             // 비밀번호 input 태그의 name 속성을 "password"로 지정
                .defaultSuccessUrl("/", true)              // 로그인 성공 시 기본으로 이동할 홈페이지 URL
                .permitAll()
            )
            
            // 4. 로그아웃 설정
            .logout(logout -> logout
                .logoutUrl("/auth/logout")                 // 로그아웃 요청 주소 (GET/POST)
                .logoutSuccessUrl("/auth/login?logout")    // 로그아웃 성공 시 다시 로그인 페이지로 리다이렉트
                .invalidateHttpSession(true)               // 로그아웃 시 HTTP 세션 초기화
                .deleteCookies("JSESSIONID")               // 브라우저 세션 쿠키 삭제
                .permitAll()
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // 회원 비밀번호 데이터베이스 저장 및 검증용 해시 암호화 인코더
        return new BCryptPasswordEncoder();
    }
}
