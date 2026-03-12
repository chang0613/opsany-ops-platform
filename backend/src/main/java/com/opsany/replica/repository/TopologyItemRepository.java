package com.opsany.replica.repository;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import com.opsany.replica.domain.TopologyItem;

@Mapper
public interface TopologyItemRepository {

    @Insert({"insert into monitor_topology_items",
        "(topology_code, name, type, node_count, link_count, abnormal, auto_refresh, updated_at, created_at)",
        "values(#{topologyCode}, #{name}, #{type}, #{nodeCount}, #{linkCount}, #{abnormal}, #{autoRefresh}, #{updatedAt}, #{createdAt})"})
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TopologyItem item);

    @Select("select id, topology_code, name, type, node_count, link_count, abnormal, auto_refresh, updated_at, created_at from monitor_topology_items order by id")
    List<TopologyItem> findAll();

    @Select("select count(*) from monitor_topology_items")
    long count();
}
