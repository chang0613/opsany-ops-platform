package com.opsany.replica.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.opsany.replica.domain.AppUser;

@Mapper
public interface AppUserRepository {

    @Select({
        "select id, username, display_name, password_hash, status, created_at, enabled, last_login_at",
        "from app_users where username = #{username} limit 1"
    })
    AppUser findByUsername(@Param("username") String username);

    @Select({
        "select id, username, display_name, password_hash, status, created_at, enabled, last_login_at",
        "from app_users where id = #{id} limit 1"
    })
    AppUser findById(@Param("id") Long id);

    @Select({
        "select r.role_code",
        "from app_user_roles ur",
        "join app_roles r on r.role_code = ur.role_code",
        "where ur.user_id = #{userId}",
        "order by r.sort_no asc, r.role_code asc"
    })
    List<String> findRoleCodes(@Param("userId") Long userId);

    @Select("select count(1) from app_users where username = #{username}")
    long countByUsername(@Param("username") String username);

    @Insert({
        "insert into app_users(username, display_name, password_hash, status, created_at, enabled, last_login_at)",
        "values(#{username}, #{displayName}, #{passwordHash}, #{status}, #{createdAt}, #{enabled}, #{lastLoginAt})"
    })
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AppUser user);

    @Update("update app_users set last_login_at = #{lastLoginAt} where id = #{userId}")
    int updateLastLoginAt(@Param("userId") Long userId, @Param("lastLoginAt") LocalDateTime lastLoginAt);
}
