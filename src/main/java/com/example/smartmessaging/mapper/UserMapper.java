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
}
