package com.opsany.replica.repository;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.opsany.replica.domain.MetricSnapshot;

@Mapper
public interface MetricSnapshotRepository {

    @Insert({
        "insert into monitor_metric_snapshots(device_id, device_code, metric_type, metric_name, metric_value, collected_at)",
        "values(#{deviceId}, #{deviceCode}, #{metricType}, #{metricName}, #{metricValue}, now())"
    })
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(MetricSnapshot snapshot);

    @Select({
        "select id, device_id, device_code, metric_type, metric_name, metric_value, collected_at",
        "from monitor_metric_snapshots where device_id = #{deviceId} order by collected_at desc limit 50"
    })
    List<MetricSnapshot> findRecentByDeviceId(@Param("deviceId") Long deviceId);

    @Select({
        "select id, device_id, device_code, metric_type, metric_name, metric_value, collected_at",
        "from monitor_metric_snapshots order by collected_at desc limit 20"
    })
    List<MetricSnapshot> findRecent();

    @Select("select count(*) from monitor_metric_snapshots")
    long count();
}
