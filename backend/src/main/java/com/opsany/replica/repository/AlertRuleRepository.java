package com.opsany.replica.repository;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import com.opsany.replica.domain.AlertRule;

@Mapper
public interface AlertRuleRepository {

    @Insert({
        "insert into monitor_alert_rules",
        "(rule_code, name, metric_type, condition_expr, threshold, severity, enabled, notify_group, description)",
        "values(#{ruleCode}, #{name}, #{metricType}, #{conditionExpr}, #{threshold}, #{severity},",
        "#{enabled}, #{notifyGroup}, #{description})"
    })
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AlertRule rule);

    @Select("select id, rule_code, name, metric_type, condition_expr, threshold, severity, enabled, notify_group, description, created_at, updated_at from monitor_alert_rules order by id")
    List<AlertRule> findAll();

    @Select("select count(*) from monitor_alert_rules")
    long count();

    @Select("select count(*) from monitor_alert_rules where enabled = true")
    long countEnabled();
}
