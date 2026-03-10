package com.opsany.replica.repository;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.opsany.replica.domain.WorkOrderCatalog;

@Mapper
public interface WorkOrderCatalogRepository {

    @Select("select count(1) from work_order_catalogs where catalog_code = #{catalogCode}")
    long countByCatalogCode(@Param("catalogCode") String catalogCode);

    @Select("select count(1) from work_order_catalogs")
    long count();

    @Insert({
        "insert into work_order_catalogs(",
        "catalog_code, name, category, type, scope, online, process_code, sla_name, owner_username, owner_display_name,",
        "description, sort_no, created_at, updated_at",
        ") values (",
        "#{catalogCode}, #{name}, #{category}, #{type}, #{scope}, #{online}, #{processCode}, #{slaName}, #{ownerUsername},",
        "#{ownerDisplayName}, #{description}, #{sortNo}, #{createdAt}, #{updatedAt}",
        ")"
    })
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(WorkOrderCatalog catalog);

    @Update({
        "update work_order_catalogs set name = #{name}, category = #{category}, type = #{type}, scope = #{scope},",
        "online = #{online}, process_code = #{processCode}, sla_name = #{slaName}, owner_username = #{ownerUsername},",
        "owner_display_name = #{ownerDisplayName}, description = #{description}, sort_no = #{sortNo}, updated_at = #{updatedAt}",
        "where catalog_code = #{catalogCode}"
    })
    int update(WorkOrderCatalog catalog);

    @Delete("delete from work_order_catalogs where catalog_code = #{catalogCode}")
    int deleteByCatalogCode(@Param("catalogCode") String catalogCode);

    @Select({
        "select id, catalog_code, name, category, type, scope, online, process_code, sla_name, owner_username, owner_display_name,",
        "description, sort_no, created_at, updated_at",
        "from work_order_catalogs order by sort_no asc, created_at asc"
    })
    List<WorkOrderCatalog> findAll();

    @Select({
        "select id, catalog_code, name, category, type, scope, online, process_code, sla_name, owner_username, owner_display_name,",
        "description, sort_no, created_at, updated_at",
        "from work_order_catalogs where online = 1 order by sort_no asc, created_at asc"
    })
    List<WorkOrderCatalog> findOnlineCatalogs();

    @Select({
        "select id, catalog_code, name, category, type, scope, online, process_code, sla_name, owner_username, owner_display_name,",
        "description, sort_no, created_at, updated_at",
        "from work_order_catalogs where catalog_code = #{catalogCode} limit 1"
    })
    WorkOrderCatalog findByCatalogCode(@Param("catalogCode") String catalogCode);

    @Select({
        "select id, catalog_code, name, category, type, scope, online, process_code, sla_name, owner_username, owner_display_name,",
        "description, sort_no, created_at, updated_at",
        "from work_order_catalogs where name = #{name} limit 1"
    })
    WorkOrderCatalog findByName(@Param("name") String name);
}
