package com.opsany.replica.repository;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import com.opsany.replica.domain.TopologyLink;

@Mapper
public interface TopologyLinkRepository {

    @Insert({"insert into monitor_topology_links",
        "(topology_code, source_node, target_node, link_type, latency, error_rate, status)",
        "values(#{topologyCode}, #{sourceNode}, #{targetNode}, #{linkType}, #{latency}, #{errorRate}, #{status})"})
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TopologyLink link);

    @Select("select id, topology_code, source_node, target_node, link_type, latency, error_rate, status from monitor_topology_links order by id")
    List<TopologyLink> findAll();

    @Select("select count(*) from monitor_topology_links")
    long count();

    @Select("select count(*) from monitor_topology_links where status = #{status}")
    long countByStatus(String status);
}
