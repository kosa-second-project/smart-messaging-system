package com.example.smartmessaging.security;

import com.example.smartmessaging.dto.vo.UsersVO;
import com.example.smartmessaging.service.repository.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("로그인 시도");

        // 1. 화면에서 입력한 사원번호(문자열)를 숫자(Integer)로 변환
        Integer empNum;
        try {
            empNum = Integer.parseInt(username);
        } catch (NumberFormatException e) {
            log.warn("올바르지 않은 사원번호 포맷");
            throw new UsernameNotFoundException("사원번호는 숫자 형식이어야 합니다.");
        }

        // 2. DB에서 활성 상태의 사원 정보 조회
        UsersVO usersVO = userMapper.findByEmpNum(empNum);
        if (usersVO == null) {
            log.warn("사원번호 존재하지 않거나 비활성화 상태");
            throw new UsernameNotFoundException("존재하지 않거나 비활성화된 사원번호입니다.");
        }

        // 3. 해당 사원의 권한(Role) 목록 조회
        List<String> roles = userMapper.findAuthoritiesByUserId(usersVO.getUserId());
        log.info("사원 조회 완료 - 권한수: {}", roles.size());


        // 4. 시큐리티 세션에 담을 CustomUserDetails 반환
        return new CustomUserDetails(usersVO, roles);
    }
}
