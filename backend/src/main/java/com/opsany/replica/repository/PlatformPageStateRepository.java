package com.opsany.replica.repository;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.opsany.replica.domain.PlatformPageState;

@Mapper
public interface PlatformPageStateRepository {

    @Select({
        "select id, platform_key, page_key, state_json, updated_by, updated_at",
        "from platform_page_states",
        "where platform_key = #{platformKey}",
        "order by page_key asc, id asc"
    })
    List<PlatformPageState> findByPlatformKey(@Param("platformKey") String platformKey);

    @Select({
        "select id, platform_key, page_key, state_json, updated_by, updated_at",
        "from platform_page_states",
        "where platform_key = #{platformKey} and page_key = #{pageKey}",
        "limit 1"
    })
    PlatformPageState findByPlatformKeyAndPageKey(@Param("platformKey") String platformKey, @Param("pageKey") String pageKey);

    @Insert({
        "insert into platform_page_states(",
        "platform_key, page_key, state_json, updated_by, updated_at",
        ") values (",
        "#{platformKey}, #{pageKey}, #{stateJson}, #{updatedBy}, #{updatedAt}",
        ")"
    })
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PlatformPageState state);

    @Update({
        "update platform_page_states",
        "set state_json = #{stateJson}, updated_by = #{updatedBy}, updated_at = #{updatedAt}",
        "where platform_key = #{platformKey} and page_key = #{pageKey}"
    })
    int update(PlatformPageState state);
}
