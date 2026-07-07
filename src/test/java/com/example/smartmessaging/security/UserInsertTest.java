package com.example.smartmessaging.security;

import com.example.smartmessaging.dto.vo.AuthoritiesVO;
import com.example.smartmessaging.dto.vo.UsersVO;
import com.example.smartmessaging.service.repository.UserMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled("로컬 DB 연동이 필요한 테스트이므로 자동 CI/CD 빌드 시 실행에서 제외합니다.")
@SpringBootTest
public class UserInsertTest {



    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 테스트 실행 시 스프링 시큐리티 세션에 임시 인증 사용자를 강제로 주입합니다. (MyBatisAuditInterceptor 통과용)
     */
    private void setupMockAuthentication() {
        UsersVO mockUser = UsersVO.builder()
                .userId(1L) // 등록자 ID를 1로 가상 지정
                .empNum(9999)
                .name("테스트관리자")
                .isActive(true)
                .build();
        
        CustomUserDetails userDetails = new CustomUserDetails(mockUser, java.util.List.of("ROLE_USER"));
        
        org.springframework.security.authentication.UsernamePasswordAuthenticationToken authentication =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()
                );
        
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    @Commit // JUnit 테스트 통과 후 데이터베이스에 INSERT가 그대로 Commit 되도록 설정
    @Transactional
    public void insertTestUserAndAuthority() {
        // 가짜 시큐리티 인증 객체 바인딩 수행 (MyBatisAuditInterceptor 우회용)
        setupMockAuthentication();

        // 1. 비밀번호 "0000"을 시큐리티 BCrypt 형식으로 암호화
        String rawPassword = "0000";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // 2. 테스트 사원 객체(UsersVO) 빌드 (사원번호: 1001, 이름: 홍길동)
        UsersVO testUser = UsersVO.builder()
                .empNum(1001)
                .pwd(encodedPassword)
                .name("홍길동")
                .isActive(true)
                .build();

        // 3. 공통 Audit 필드는 MyBatisAuditInterceptor에 의해 자동으로 주입되므로 자바 코드에서 생략 가능합니다.

        // 4. USERS 테이블에 사원 INSERT 실행
        int userInsertResult = userMapper.insertUser(testUser);
        assertTrue(userInsertResult > 0, "사원 정보가 등록되어야 합니다.");
        assertNotNull(testUser.getUserId(), "자동 생성된 사원 시퀀스 ID(PK)를 반환받아야 합니다.");

        System.out.println("생성된 사원 PK ID: " + testUser.getUserId());

        // 5. AUTHORITIES 테이블에 권한 INSERT 실행 (ROLE_USER 권한 부여)
        AuthoritiesVO authority = AuthoritiesVO.builder()
                .userId(testUser.getUserId())
                .authority("ROLE_USER")
                .build();
        
        int authInsertResult = userMapper.insertAuthority(authority);
        assertTrue(authInsertResult > 0, "사원 권한이 매핑되어야 합니다.");

        System.out.println("임시 테스트용 사원 데이터 입력 성공!");
        System.out.println("아이디(사원번호): 1001 | 비밀번호: 0000");
    }
}


