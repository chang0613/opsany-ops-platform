package com.opsany.replica.repository;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import com.opsany.replica.domain.ArchiveHistory;

@Mapper
public interface ArchiveHistoryRepository {

    @Insert({"insert into monitor_archive_history",
        "(policy_code, policy, data_size, duration, status, executed_at)",
        "values(#{policyCode}, #{policy}, #{dataSize}, #{duration}, #{status}, #{executedAt})"})
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ArchiveHistory history);

    @Select("select id, policy_code, policy, data_size, duration, status, executed_at from monitor_archive_history order by executed_at desc")
    List<ArchiveHistory> findAll();

    @Select("select count(*) from monitor_archive_history")
    long count();
}
