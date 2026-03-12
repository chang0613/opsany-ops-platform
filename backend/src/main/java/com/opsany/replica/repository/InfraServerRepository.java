package com.opsany.replica.repository;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import com.opsany.replica.domain.InfraServer;

@Mapper
public interface InfraServerRepository {

    @Insert({"insert into monitor_infra_servers",
        "(server_code, hostname, ip, model, cpu_usage, mem_usage, disk_usage, cpu_temp, collect_method, status, last_collected_at, description, created_at, updated_at)",
        "values(#{serverCode}, #{hostname}, #{ip}, #{model}, #{cpuUsage}, #{memUsage}, #{diskUsage}, #{cpuTemp}, #{collectMethod}, #{status}, #{lastCollectedAt}, #{description}, #{createdAt}, #{updatedAt})"})
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(InfraServer server);

    @Select("select id, server_code, hostname, ip, model, cpu_usage, mem_usage, disk_usage, cpu_temp, collect_method, status, last_collected_at, description, created_at, updated_at from monitor_infra_servers order by id")
    List<InfraServer> findAll();

    @Select("select count(*) from monitor_infra_servers")
    long count();

    @Select("select count(*) from monitor_infra_servers where status = #{status}")
    long countByStatus(String status);
}
