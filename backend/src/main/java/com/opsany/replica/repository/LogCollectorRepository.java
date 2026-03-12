package com.opsany.replica.repository;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import com.opsany.replica.domain.LogCollector;

@Mapper
public interface LogCollectorRepository {

    @Insert({
        "insert into monitor_log_collectors",
        "(collector_code, name, source_type, source_path, target_host, encoding, status, lines_collected, description)",
        "values(#{collectorCode}, #{name}, #{sourceType}, #{sourcePath}, #{targetHost}, #{encoding},",
        "#{status}, #{linesCollected}, #{description})"
    })
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(LogCollector collector);

    @Select("select id, collector_code, name, source_type, source_path, target_host, encoding, status, lines_collected, last_collected_at, description, created_at, updated_at from monitor_log_collectors order by id")
    List<LogCollector> findAll();

    @Select("select count(*) from monitor_log_collectors")
    long count();

    @Select("select count(*) from monitor_log_collectors where status = #{status}")
    long countByStatus(String status);
}
