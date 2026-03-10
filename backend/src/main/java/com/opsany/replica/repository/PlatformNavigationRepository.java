package com.opsany.replica.repository;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.opsany.replica.domain.PlatformNavigationGroup;
import com.opsany.replica.domain.PlatformNavigationItem;
import com.opsany.replica.dto.NavigationItemDto;

@Mapper
public interface PlatformNavigationRepository {

    @Select({
        "select id, group_code, title, sort_no",
        "from platform_navigation_groups",
        "order by sort_no asc, id asc"
    })
    List<PlatformNavigationGroup> findAllGroups();

    @Select("select count(1) from platform_navigation_groups")
    long countGroups();

    @Select("select count(1) from platform_navigation_groups where group_code = #{groupCode}")
    long countByGroupCode(@Param("groupCode") String groupCode);

    @Insert({
        "insert into platform_navigation_groups(group_code, title, sort_no)",
        "values(#{groupCode}, #{title}, #{sortNo})"
    })
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertGroup(PlatformNavigationGroup group);

    @Update({
        "update platform_navigation_groups set title = #{title}, sort_no = #{sortNo}",
        "where group_code = #{groupCode}"
    })
    int updateGroup(PlatformNavigationGroup group);

    @Select("select count(1) from platform_navigation_items")
    long countItems();

    @Select("select count(1) from platform_navigation_items where item_code = #{itemCode}")
    long countByItemCode(@Param("itemCode") String itemCode);

    @Insert({
        "insert into platform_navigation_items(",
        "item_code, group_code, name, icon, creator_username, creator_display_name, link, mobile_visible, description, sort_no, enabled",
        ") values (",
        "#{itemCode}, #{groupCode}, #{name}, #{icon}, #{creatorUsername}, #{creatorDisplayName}, #{link},",
        "#{mobileVisible}, #{description}, #{sortNo}, #{enabled}",
        ")"
    })
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertItem(PlatformNavigationItem item);

    @Update({
        "update platform_navigation_items set group_code = #{groupCode}, name = #{name}, icon = #{icon},",
        "creator_username = #{creatorUsername}, creator_display_name = #{creatorDisplayName}, link = #{link},",
        "mobile_visible = #{mobileVisible}, description = #{description}, sort_no = #{sortNo}, enabled = #{enabled}",
        "where item_code = #{itemCode}"
    })
    int updateItem(PlatformNavigationItem item);

    @Delete("delete from platform_navigation_items where item_code = #{itemCode}")
    int deleteItemByCode(@Param("itemCode") String itemCode);

    @Delete("delete from app_user_navigation_favorites where item_code = #{itemCode}")
    int deleteFavoritesByItemCode(@Param("itemCode") String itemCode);

    @Select({
        "select i.id, i.item_code as itemCode, i.group_code as groupCode, g.title as groupTitle, g.sort_no as groupSortNo,",
        "i.name, i.icon, i.creator_username as creatorUsername, i.creator_display_name as creatorDisplayName,",
        "i.link, i.mobile_visible as mobileVisible, i.description, i.sort_no as sortNo, i.enabled",
        "from platform_navigation_items i",
        "join platform_navigation_groups g on g.group_code = i.group_code",
        "order by g.sort_no asc, i.sort_no asc, i.id asc"
    })
    List<NavigationItemDto> findAllItemDtos();

    @Select({
        "select item_code",
        "from app_user_navigation_favorites",
        "where user_id = #{userId}",
        "order by sort_no asc, id asc"
    })
    List<String> findFavoriteItemCodesByUserId(@Param("userId") Long userId);

    @Select("select count(1) from app_user_navigation_favorites where user_id = #{userId} and item_code = #{itemCode}")
    long countFavorite(@Param("userId") Long userId, @Param("itemCode") String itemCode);

    @Insert("insert into app_user_navigation_favorites(user_id, item_code, sort_no) values(#{userId}, #{itemCode}, #{sortNo})")
    int insertFavorite(@Param("userId") Long userId, @Param("itemCode") String itemCode, @Param("sortNo") Integer sortNo);

    @Delete("delete from app_user_navigation_favorites where user_id = #{userId} and item_code = #{itemCode}")
    int deleteFavorite(@Param("userId") Long userId, @Param("itemCode") String itemCode);

    @Select("select coalesce(max(sort_no), 0) from app_user_navigation_favorites where user_id = #{userId}")
    Integer findMaxFavoriteSortNo(@Param("userId") Long userId);
}
