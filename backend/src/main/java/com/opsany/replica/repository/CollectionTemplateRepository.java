package com.opsany.replica.repository;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.opsany.replica.domain.CollectionTemplate;

@Mapper
public interface CollectionTemplateRepository {

    @Insert({
        "insert into monitor_collection_templates(",
        "template_code, name, device_type, protocol, metrics_json, interval_seconds, enabled, description, created_at, updated_at",
        ") values (",
        "#{templateCode}, #{name}, #{deviceType}, #{protocol}, #{metricsJson}, #{intervalSeconds}, #{enabled}, #{description}, now(), now()",
        ")"
    })
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CollectionTemplate template);

    @Update({
        "update monitor_collection_templates set",
        "name = #{name}, device_type = #{deviceType}, protocol = #{protocol},",
        "metrics_json = #{metricsJson}, interval_seconds = #{intervalSeconds},",
        "enabled = #{enabled}, description = #{description}, updated_at = now()",
        "where id = #{id}"
    })
    int update(CollectionTemplate template);

    @Select({
        "select id, template_code, name, device_type, protocol, metrics_json, interval_seconds, enabled, description, created_at, updated_at",
        "from monitor_collection_templates order by id asc"
    })
    List<CollectionTemplate> findAll();

    @Select({
        "select id, template_code, name, device_type, protocol, metrics_json, interval_seconds, enabled, description, created_at, updated_at",
        "from monitor_collection_templates where id = #{id} limit 1"
    })
    CollectionTemplate findById(@Param("id") Long id);

    @Select({
        "select id, template_code, name, device_type, protocol, metrics_json, interval_seconds, enabled, description, created_at, updated_at",
        "from monitor_collection_templates where template_code = #{templateCode} limit 1"
    })
    CollectionTemplate findByTemplateCode(@Param("templateCode") String templateCode);

    @Select("select count(1) from monitor_collection_templates")
    long count();

    @Delete("delete from monitor_collection_templates where id = #{id}")
    int deleteById(@Param("id") Long id);
}
