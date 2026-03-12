package com.opsany.replica.repository;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import com.opsany.replica.domain.BigScreen;

@Mapper
public interface BigScreenRepository {

    @Insert({"insert into monitor_bigscreens",
        "(screen_code, name, resolution, charts, auto_play, shared, visit_today, updated_at, created_at)",
        "values(#{screenCode}, #{name}, #{resolution}, #{charts}, #{autoPlay}, #{shared}, #{visitToday}, #{updatedAt}, #{createdAt})"})
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(BigScreen screen);

    @Select("select id, screen_code, name, resolution, charts, auto_play, shared, visit_today, updated_at, created_at from monitor_bigscreens order by id")
    List<BigScreen> findAll();

    @Select("select count(*) from monitor_bigscreens")
    long count();
}
