package com.opsany.replica.repository;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import com.opsany.replica.domain.ServiceProbe;

@Mapper
public interface ServiceProbeRepository {

    @Insert({
        "insert into monitor_service_probes",
        "(probe_code, name, probe_type, target_url, protocol, interval_seconds, timeout_ms, status, last_result, description)",
        "values(#{probeCode}, #{name}, #{probeType}, #{targetUrl}, #{protocol}, #{intervalSeconds},",
        "#{timeoutMs}, #{status}, #{lastResult}, #{description})"
    })
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ServiceProbe probe);

    @Select("select id, probe_code, name, probe_type, target_url, protocol, interval_seconds, timeout_ms, status, last_result, last_checked_at, description, created_at, updated_at from monitor_service_probes order by id")
    List<ServiceProbe> findAll();

    @Select("select count(*) from monitor_service_probes")
    long count();

    @Select("select count(*) from monitor_service_probes where status = #{status}")
    long countByStatus(String status);
}
