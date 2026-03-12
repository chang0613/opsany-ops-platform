package com.opsany.replica.repository;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import com.opsany.replica.domain.ResponseRecord;

@Mapper
public interface ResponseRecordRepository {

    @Insert({"insert into monitor_response_records",
        "(alert_name, responder, response_min, resolve_min, result, triggered_at)",
        "values(#{alertName}, #{responder}, #{responseMin}, #{resolveMin}, #{result}, #{triggeredAt})"})
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ResponseRecord record);

    @Select("select id, alert_name, responder, response_min, resolve_min, result, triggered_at from monitor_response_records order by triggered_at desc")
    List<ResponseRecord> findAll();

    @Select("select count(*) from monitor_response_records")
    long count();
}
