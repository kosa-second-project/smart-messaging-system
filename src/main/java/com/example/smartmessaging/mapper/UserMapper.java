package com.example.smartmessaging.mapper;

import com.example.smartmessaging.dto.vo.UsersVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface UserMapper {

    /**
 * Retrieves an active employee's information by employee number.
 *
 * @param empNum the employee number
 * @return the matching user information
 */
    UsersVO findByEmpNum(@Param("empNum") Integer empNum);

    /**
 * Retrieves the authority list assigned to a user.
 *
 * @param userId the ID of the user
 * @return the authority strings assigned to the specified user
 */
    List<String> findAuthoritiesByUserId(@Param("userId") Long userId);

    /**
 * Inserts a new user record into the database.
 *
 * @param usersVO the user information to store
 * @return the number of rows affected
 */
    int insertUser(UsersVO usersVO);

    /**
 * Assigns a authority to the specified user.
 *
 * @param userId the user identifier
 * @param authority the authority to assign
 * @return the number of rows affected
 */
    int insertAuthority(@Param("userId") Long userId, @Param("authority") String authority);
}

