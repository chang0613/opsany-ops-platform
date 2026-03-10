package com.opsany.replica.repository;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import com.opsany.replica.domain.LoginAudit;

@Mapper
public interface LoginAuditRepository {

    @Insert({
        "insert into login_audits(user_id, username, login_ip, user_agent, login_at)",
        "values(#{userId}, #{username}, #{loginIp}, #{userAgent}, #{loginAt})"
    })
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(LoginAudit loginAudit);

    @Select("select count(1) from login_audits")
    long count();
}
