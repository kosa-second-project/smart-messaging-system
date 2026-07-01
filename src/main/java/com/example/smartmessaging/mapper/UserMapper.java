package com.example.smartmessaging.mapper;

import com.example.smartmessaging.dto.vo.UsersVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface UserMapper {

    /**
     * 사원번호(empNum)로 활성화된 사원 정보를 조회합니다.
     */
    UsersVO findByEmpNum(@Param("empNum") Integer empNum);

    /**
     * 사용자 ID(userId)에 할당된 권한 목록을 조회합니다.
     */
    List<String> findAuthoritiesByUserId(@Param("userId") Long userId);

    /**
     * 신규 사원을 데이터베이스에 등록합니다. (회원가입/사원생성 용)
     */
    int insertUser(UsersVO usersVO);

    /**
     * 특정 사원에게 권한을 할당합니다.
     */
    int insertAuthority(@Param("userId") Long userId, @Param("authority") String authority);
}

