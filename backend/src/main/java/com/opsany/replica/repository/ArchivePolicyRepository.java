package com.opsany.replica.repository;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import com.opsany.replica.domain.ArchivePolicy;

@Mapper
public interface ArchivePolicyRepository {

    @Insert({"insert into monitor_archive_policies",
        "(policy_code, policy_name, target_type, hot_days, warm_days, cold_days, compress, status, created_at, updated_at)",
        "values(#{policyCode}, #{policyName}, #{targetType}, #{hotDays}, #{warmDays}, #{coldDays}, #{compress}, #{status}, #{createdAt}, #{updatedAt})"})
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ArchivePolicy policy);

    @Select("select id, policy_code, policy_name, target_type, hot_days, warm_days, cold_days, compress, status, created_at, updated_at from monitor_archive_policies order by id")
    List<ArchivePolicy> findAll();

    @Select("select count(*) from monitor_archive_policies")
    long count();
}
