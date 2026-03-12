package com.opsany.replica.repository;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import com.opsany.replica.domain.MiddlewareInstance;

@Mapper
public interface MiddlewareInstanceRepository {

    @Insert({"insert into monitor_middleware_instances",
        "(instance_code, name, middleware_type, version, host, port, cpu_usage, mem_usage, connect_count, qps, status, last_collected_at, created_at, updated_at)",
        "values(#{instanceCode}, #{name}, #{middlewareType}, #{version}, #{host}, #{port}, #{cpuUsage}, #{memUsage}, #{connectCount}, #{qps}, #{status}, #{lastCollectedAt}, #{createdAt}, #{updatedAt})"})
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(MiddlewareInstance instance);

    @Select("select id, instance_code, name, middleware_type, version, host, port, cpu_usage, mem_usage, connect_count, qps, status, last_collected_at, created_at, updated_at from monitor_middleware_instances order by id")
    List<MiddlewareInstance> findAll();

    @Select("select count(*) from monitor_middleware_instances")
    long count();

    @Select("select count(*) from monitor_middleware_instances where status = #{status}")
    long countByStatus(String status);
}
