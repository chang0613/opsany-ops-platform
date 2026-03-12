package com.opsany.replica.repository;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import com.opsany.replica.domain.AlertEvent;

@Mapper
public interface AlertEventRepository {

    @Insert({"insert into monitor_alert_events",
        "(event_code, rule_name, source, severity, status, handler, triggered_at, resolved_at)",
        "values(#{eventCode}, #{ruleName}, #{source}, #{severity}, #{status}, #{handler}, #{triggeredAt}, #{resolvedAt})"})
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AlertEvent event);

    @Select("select id, event_code, rule_name, source, severity, status, handler, triggered_at, resolved_at from monitor_alert_events order by triggered_at desc")
    List<AlertEvent> findAll();

    @Select("select count(*) from monitor_alert_events")
    long count();

    @Select("select count(*) from monitor_alert_events where status = #{status}")
    long countByStatus(String status);
}
