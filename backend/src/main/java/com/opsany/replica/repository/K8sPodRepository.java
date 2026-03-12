package com.opsany.replica.repository;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import com.opsany.replica.domain.K8sPod;

@Mapper
public interface K8sPodRepository {

    @Insert({"insert into monitor_k8s_pods",
        "(pod_code, cluster_code, namespace, pod_name, node_name, cpu_usage, mem_usage, restart_count, status, created_at)",
        "values(#{podCode}, #{clusterCode}, #{namespace}, #{podName}, #{nodeName}, #{cpuUsage}, #{memUsage}, #{restartCount}, #{status}, #{createdAt})"})
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(K8sPod pod);

    @Select("select id, pod_code, cluster_code, namespace, pod_name, node_name, cpu_usage, mem_usage, restart_count, status, created_at from monitor_k8s_pods order by id")
    List<K8sPod> findAll();

    @Select("select count(*) from monitor_k8s_pods")
    long count();

    @Select("select count(*) from monitor_k8s_pods where status = #{status}")
    long countByStatus(String status);
}
