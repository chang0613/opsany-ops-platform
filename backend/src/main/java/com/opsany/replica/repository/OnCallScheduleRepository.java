package com.opsany.replica.repository;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import com.opsany.replica.domain.OnCallSchedule;

@Mapper
public interface OnCallScheduleRepository {

    @Insert({"insert into monitor_oncall_schedules",
        "(duty_date, weekday, group_name, person, shift, status)",
        "values(#{dutyDate}, #{weekday}, #{groupName}, #{person}, #{shift}, #{status})"})
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(OnCallSchedule schedule);

    @Select("select id, duty_date, weekday, group_name, person, shift, status from monitor_oncall_schedules order by duty_date")
    List<OnCallSchedule> findAll();

    @Select("select count(*) from monitor_oncall_schedules")
    long count();
}
