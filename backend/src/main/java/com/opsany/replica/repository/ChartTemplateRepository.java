package com.opsany.replica.repository;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import com.opsany.replica.domain.ChartTemplate;

@Mapper
public interface ChartTemplateRepository {

    @Insert({"insert into monitor_chart_templates",
        "(template_code, template_name, chart_type, category, used_count, created_at, updated_at)",
        "values(#{templateCode}, #{templateName}, #{chartType}, #{category}, #{usedCount}, #{createdAt}, #{updatedAt})"})
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ChartTemplate template);

    @Select("select id, template_code, template_name, chart_type, category, used_count, created_at, updated_at from monitor_chart_templates order by id")
    List<ChartTemplate> findAll();

    @Select("select count(*) from monitor_chart_templates")
    long count();
}
