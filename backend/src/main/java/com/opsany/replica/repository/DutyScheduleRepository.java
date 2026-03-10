package com.opsany.replica.repository;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.opsany.replica.domain.DutyGroup;
import com.opsany.replica.domain.DutyShift;

@Mapper
public interface DutyScheduleRepository {

    @Select("select count(1) from duty_groups where name = #{name}")
    long countGroupByName(@Param("name") String name);

    @Insert({
        "insert into duty_groups(name, owner_username, owner_display_name, members, coverage, description)",
        "values(#{name}, #{ownerUsername}, #{ownerDisplayName}, #{members}, #{coverage}, #{description})"
    })
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertGroup(DutyGroup dutyGroup);

    @Select({
        "select id, name, owner_username, owner_display_name, members, coverage, description",
        "from duty_groups order by id asc"
    })
    List<DutyGroup> findAllGroups();

    @Select("select count(1) from duty_schedules where duty_date = #{dutyDate} and group_id = #{groupId}")
    long countShiftByDateAndGroup(@Param("groupId") Long groupId, @Param("dutyDate") String dutyDate);

    @Insert({
        "insert into duty_schedules(",
        "group_id, group_name, duty_date, date_label, shift_label, shift_time, owner_username, owner_display_name, status",
        ") values (",
        "#{groupId}, #{groupName}, #{dutyDate}, #{dateLabel}, #{shiftLabel}, #{shiftTime}, #{ownerUsername},",
        "#{ownerDisplayName}, #{status}",
        ")"
    })
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertShift(DutyShift dutyShift);

    @Select({
        "select id, group_id, group_name, duty_date, date_label, shift_label, shift_time, owner_username, owner_display_name, status",
        "from duty_schedules order by duty_date asc, id asc"
    })
    List<DutyShift> findAllShifts();

    @Select({
        "select id, group_id, group_name, duty_date, date_label, shift_label, shift_time, owner_username, owner_display_name, status",
        "from duty_schedules where owner_username = #{username} order by duty_date asc, id asc"
    })
    List<DutyShift> findByOwnerUsername(@Param("username") String username);
}
