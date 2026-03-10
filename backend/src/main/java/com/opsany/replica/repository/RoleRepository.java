package com.opsany.replica.repository;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RoleRepository {

    @Select("select count(1) from app_roles where role_code = #{roleCode}")
    long countByRoleCode(@Param("roleCode") String roleCode);

    @Insert({
        "insert into app_roles(role_code, role_name, description, sort_no)",
        "values(#{roleCode}, #{roleName}, #{description}, #{sortNo})"
    })
    int insertRole(
        @Param("roleCode") String roleCode,
        @Param("roleName") String roleName,
        @Param("description") String description,
        @Param("sortNo") Integer sortNo
    );

    @Select("select count(1) from app_user_roles where user_id = #{userId} and role_code = #{roleCode}")
    long countUserRole(@Param("userId") Long userId, @Param("roleCode") String roleCode);

    @Insert("insert into app_user_roles(user_id, role_code) values(#{userId}, #{roleCode})")
    int bindUserRole(@Param("userId") Long userId, @Param("roleCode") String roleCode);
}
