package com.example.smartmessaging.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
public class AuthController {

    /**
     * 커스텀 로그인 화면을 반환합니다.
     * 로그인 실패(error) 또는 로그아웃(logout) 시 리다이렉션 매핑을 함께 제어합니다.
     */
    @GetMapping("/auth/login")
    public String loginPage(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            HttpServletRequest request,
            Model model) {

        // 1. 로그인 실패 시 에러 메시지 추출
        if (error != null) {
            HttpSession session = request.getSession(false);
            String errorMessage = "사원번호 또는 비밀번호를 다시 확인해주세요.";
            
            if (session != null) {
                // 스프링 시큐리티가 실패 시 세션에 저장해 둔 예외(Exception) 객체 추출
                Object exceptionObj = session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
                if (exceptionObj instanceof AuthenticationException) {
                    AuthenticationException exception = (AuthenticationException) exceptionObj;
                    log.warn("로그인 실패 예외: {}", exception.getMessage());
                    
                    // 시큐리티 예외 메시지에 따라 가독성 좋은 한글 안내로 매핑
                    if (exception.getMessage().contains("존재하지 않거나 비활성화")) {
                        errorMessage = "존재하지 않거나 활성화되지 않은 사원번호입니다.";
                    } else if (exception.getMessage().contains("Bad credentials") || exception.getMessage().contains("비밀번호")) {
                        errorMessage = "비밀번호가 올바르지 않습니다.";
                    } else if (exception.getMessage().contains("숫자 형식")) {
                        errorMessage = "사원번호는 숫자 형식으로만 입력해 주세요.";
                    }
                }
            }
            model.addAttribute("errorMessage", errorMessage);
        }

        // 2. 로그아웃 후 진입 시 안내 메시지 전달
        if (logout != null) {
            model.addAttribute("logoutMessage", "안전하게 로그아웃되었습니다.");
        }

        return "auth/login"; // templates/auth/login.html
    }
}
