package com.opsany.replica.repository;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import com.opsany.replica.domain.K8sCluster;

@Mapper
public interface K8sClusterRepository {

    @Insert({"insert into monitor_k8s_clusters",
        "(cluster_code, cluster_name, version, node_count, pod_count, cpu_usage, mem_usage, status, last_collected_at, created_at, updated_at)",
        "values(#{clusterCode}, #{clusterName}, #{version}, #{nodeCount}, #{podCount}, #{cpuUsage}, #{memUsage}, #{status}, #{lastCollectedAt}, #{createdAt}, #{updatedAt})"})
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(K8sCluster cluster);

    @Select("select id, cluster_code, cluster_name, version, node_count, pod_count, cpu_usage, mem_usage, status, last_collected_at, created_at, updated_at from monitor_k8s_clusters order by id")
    List<K8sCluster> findAll();

    @Select("select count(*) from monitor_k8s_clusters")
    long count();

    @Select("select count(*) from monitor_k8s_clusters where status = #{status}")
    long countByStatus(String status);
}
