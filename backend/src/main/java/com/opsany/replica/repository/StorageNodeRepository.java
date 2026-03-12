package com.opsany.replica.repository;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import com.opsany.replica.domain.StorageNode;

@Mapper
public interface StorageNodeRepository {

    @Insert({"insert into monitor_storage_nodes",
        "(node_code, node, role, used, total, write_rate, query_rate, status, updated_at)",
        "values(#{nodeCode}, #{node}, #{role}, #{used}, #{total}, #{writeRate}, #{queryRate}, #{status}, #{updatedAt})"})
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(StorageNode node);

    @Select("select id, node_code, node, role, used, total, write_rate, query_rate, status, updated_at from monitor_storage_nodes order by id")
    List<StorageNode> findAll();

    @Select("select count(*) from monitor_storage_nodes")
    long count();

    @Select("select count(*) from monitor_storage_nodes where status = #{status}")
    long countByStatus(String status);
}
