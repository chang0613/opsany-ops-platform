package com.opsany.replica.repository;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import com.opsany.replica.domain.Dashboard;

@Mapper
public interface DashboardRepository {

    @Insert({"insert into monitor_dashboards",
        "(dashboard_code, name, category, charts, creator, shared, visit_today, updated_at, created_at)",
        "values(#{dashboardCode}, #{name}, #{category}, #{charts}, #{creator}, #{shared}, #{visitToday}, #{updatedAt}, #{createdAt})"})
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Dashboard dashboard);

    @Select("select id, dashboard_code, name, category, charts, creator, shared, visit_today, updated_at, created_at from monitor_dashboards order by id")
    List<Dashboard> findAll();

    @Select("select count(*) from monitor_dashboards")
    long count();
}
