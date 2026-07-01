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

    /**
     * Creates a user details wrapper for the specified user and roles.
     *
     * @param usersVO the backing user data
     * @param roles the role names to expose as granted authorities
     */
    public CustomUserDetails(UsersVO usersVO, List<String> roles) {
        this.usersVO = usersVO;
        this.roles = roles;
    }

    /**
     * Returns the underlying user value object.
     *
     * @return the wrapped {@code UsersVO}
     */
    public UsersVO getUsersVO() {
        return this.usersVO;
    }

    /**
     * Gets the underlying user's unique identifier.
     *
     * @return the user's primary key
     */
    public Long getUserId() {
        return this.usersVO.getUserId();
    }

    /**
     * Returns the employee's display name.
     *
     * @return the name stored in the underlying user record
     */
    public String getName() {
        return this.usersVO.getName();
    }

    /**
     * Returns the authorities granted to the user.
     *
     * @return the authorities derived from the stored role strings
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // DB에 저장된 권한 문자열을 시큐리티가 인식하는 GrantedAuthority 객체로 변환
        return this.roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    /**
     * Returns the stored password for this user.
     *
     * @return the user's password
     */
    @Override
    public String getPassword() {
        // 암호화된 패스워드 반환
        return this.usersVO.getPwd();
    }

    /**
     * Provides the employee number used as the login identifier.
     *
     * @return the employee number as a string
     */
    @Override
    public String getUsername() {
        // 로그인 식별 아이디로 사원번호(empNum) 문자열을 반환합니다.
        return this.usersVO.getEmpNum().toString();
    }

    /**
     * Indicates whether the account has expired.
     *
     * @return {@code true} if the account is not expired, {@code false} otherwise.
     */
    @Override
    public boolean isAccountNonExpired() {
        return true; // 계정 만료 여부 (기본 true = 만료 안됨)
    }

    /**
     * Indicates whether the account is not locked.
     *
     * @return {@code true} if the account is not locked, {@code false} otherwise.
     */
    @Override
    public boolean isAccountNonLocked() {
        return true; // 계정 잠금 여부 (기본 true = 잠기지 않음)
    }

    /**
     * Indicates whether the user's credentials remain valid.
     *
     * @return {@code true} if the credentials are still valid, {@code false} otherwise.
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true; // 자격 증명(비밀번호) 만료 여부 (기본 true)
    }

    /**
     * Determines whether the user account is enabled.
     *
     * @return {@code true} if the user is marked active, {@code false} otherwise.
     */
    @Override
    public boolean isEnabled() {
        // 사원이 활성화(isActive) 상태일 때만 로그인 허용
        // usersVO.getIsActive()가 Boolean이므로 null 체크 후 리턴
        return this.usersVO.getIsActive() != null && this.usersVO.getIsActive();
    }
}
