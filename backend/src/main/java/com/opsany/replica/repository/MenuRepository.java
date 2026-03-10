package com.opsany.replica.repository;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;

import com.opsany.replica.domain.MenuPermission;

@Mapper
public interface MenuRepository {

    @Select("select count(1) from app_menus where menu_code = #{menuCode}")
    long countByMenuCode(@Param("menuCode") String menuCode);

    @Insert({
        "insert into app_menus(",
        "menu_code, group_name, group_sort_no, label, route, icon, sort_no, permission_code, visible",
        ") values (",
        "#{menuCode}, #{groupName}, #{groupSortNo}, #{label}, #{route}, #{icon}, #{sortNo}, #{permissionCode}, #{visible}",
        ")"
    })
    int insert(MenuPermission menuPermission);

    @Update({
        "update app_menus set group_name = #{groupName}, group_sort_no = #{groupSortNo}, label = #{label},",
        "route = #{route}, icon = #{icon}, sort_no = #{sortNo}, permission_code = #{permissionCode}, visible = #{visible}",
        "where menu_code = #{menuCode}"
    })
    int update(MenuPermission menuPermission);

    @Select("select count(1) from app_role_menus where role_code = #{roleCode} and menu_code = #{menuCode}")
    long countRoleMenu(@Param("roleCode") String roleCode, @Param("menuCode") String menuCode);

    @Insert("insert into app_role_menus(role_code, menu_code) values(#{roleCode}, #{menuCode})")
    int grantRoleMenu(@Param("roleCode") String roleCode, @Param("menuCode") String menuCode);

    @Delete("delete from app_role_menus where menu_code = #{menuCode}")
    int deleteRoleMenusByMenuCode(@Param("menuCode") String menuCode);

    @Delete("delete from app_menus where menu_code = #{menuCode}")
    int deleteMenuByCode(@Param("menuCode") String menuCode);

    @Select({
        "select id, menu_code, group_name, group_sort_no, label, route, icon, sort_no, permission_code, visible",
        "from app_menus order by group_sort_no asc, sort_no asc"
    })
    List<MenuPermission> findAllMenus();

    @Select({
        "select role_code from app_role_menus",
        "where menu_code = #{menuCode}",
        "order by role_code asc"
    })
    List<String> findRoleCodesByMenuCode(@Param("menuCode") String menuCode);

    @Select({
        "select m.id, m.menu_code, m.group_name, m.group_sort_no, m.label, m.route, m.icon, m.sort_no,",
        "m.permission_code, m.visible",
        "from app_menus m",
        "join app_role_menus rm on rm.menu_code = m.menu_code",
        "join app_user_roles ur on ur.role_code = rm.role_code",
        "where ur.user_id = #{userId} and m.visible = 1",
        "group by m.id, m.menu_code, m.group_name, m.group_sort_no, m.label, m.route, m.icon, m.sort_no,",
        "m.permission_code, m.visible",
        "order by m.group_sort_no asc, m.sort_no asc"
    })
    List<MenuPermission> findMenusByUserId(@Param("userId") Long userId);

    @Select({
        "select m.route",
        "from app_menus m",
        "join app_role_menus rm on rm.menu_code = m.menu_code",
        "join app_user_roles ur on ur.role_code = rm.role_code",
        "where ur.user_id = #{userId} and m.visible = 1",
        "order by m.group_sort_no asc, m.sort_no asc",
        "limit 1"
    })
    String findFirstRouteByUserId(@Param("userId") Long userId);
}
