package com.opsany.replica.repository;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import com.opsany.replica.domain.NotifyChannel;

@Mapper
public interface NotifyChannelRepository {

    @Insert({"insert into monitor_notify_channels",
        "(channel_code, channel_name, channel_type, config, sent_today, fail_today, status, created_at, updated_at)",
        "values(#{channelCode}, #{channelName}, #{channelType}, #{config}, #{sentToday}, #{failToday}, #{status}, #{createdAt}, #{updatedAt})"})
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(NotifyChannel channel);

    @Select("select id, channel_code, channel_name, channel_type, config, sent_today, fail_today, status, created_at, updated_at from monitor_notify_channels order by id")
    List<NotifyChannel> findAll();

    @Select("select count(*) from monitor_notify_channels")
    long count();

    @Select("select count(*) from monitor_notify_channels where status = #{status}")
    long countByStatus(String status);
}
