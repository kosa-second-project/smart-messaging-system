package com.example.smartmessaging.security;

import com.example.smartmessaging.dto.vo.UsersVO;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class CustomUserDetails implements UserDetails {

    private final UsersVO usersVO;
    private final List<String> roles;

    public CustomUserDetails(UsersVO usersVO, List<String> roles) {
        this.usersVO = usersVO;
        this.roles = roles;
    }

    /**
     * 사용자의 데이터베이스 원본 VO를 꺼내올 수 있도록 헬퍼 제공 (컨트롤러나 서비스에서 사용 가능)
     */
    public UsersVO getUsersVO() {
        return this.usersVO;
    }

    /**
     * 로그인한 사원의 고유 식별자(PK)를 반환하는 헬퍼
     */
    public Long getUserId() {
        return this.usersVO.getUserId();
    }

    /**
     * 로그인한 사원의 실제 이름(한글명)을 반환하는 헬퍼
     */
    public String getName() {
        return this.usersVO.getName();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // DB에 저장된 권한 문자열(예: ROLE_USER, ROLE_ADMIN)을 시큐리티가 인식하는 GrantedAuthority 객체로 변환
        return this.roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        // 암호화된 패스워드 반환
        return this.usersVO.getPwd();
    }

    @Override
    public String getUsername() {
        // 로그인 식별 아이디로 사원번호(empNum) 문자열을 반환합니다.
        return this.usersVO.getEmpNum().toString();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // 계정 만료 여부 (기본 true = 만료 안됨)
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // 계정 잠금 여부 (기본 true = 잠기지 않음)
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // 자격 증명(비밀번호) 만료 여부 (기본 true)
    }

    @Override
    public boolean isEnabled() {
        // 사원이 활성화(isActive) 상태일 때만 로그인 허용
        // usersVO.getIsActive()가 Boolean이므로 null 체크 후 리턴
        return this.usersVO.getIsActive() != null && this.usersVO.getIsActive();
    }
}
