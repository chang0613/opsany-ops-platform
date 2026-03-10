package com.opsany.replica.repository;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.opsany.replica.domain.NotificationMessage;

@Mapper
public interface NotificationMessageRepository {

    @Results(id = "notificationMessageResult", value = {
        @Result(column = "message_type", property = "messageType"),
        @Result(column = "sent_at", property = "sentAt"),
        @Result(column = "is_read", property = "read"),
        @Result(column = "recipient_username", property = "recipientUsername"),
        @Result(column = "source_type", property = "sourceType"),
        @Result(column = "source_id", property = "sourceId")
    })
    @Select({
        "select id, title, message_type, sent_at, is_read, recipient_username, source_type, source_id",
        "from notification_messages order by sent_at desc limit 10"
    })
    List<NotificationMessage> findTop10ByOrderBySentAtDesc();

    @ResultMap("notificationMessageResult")
    @Select({
        "select id, title, message_type, sent_at, is_read, recipient_username, source_type, source_id",
        "from notification_messages order by sent_at desc limit 3"
    })
    List<NotificationMessage> findTop3ByOrderBySentAtDesc();

    @Select("select count(1) from notification_messages")
    long count();

    @Insert({
        "insert into notification_messages(title, message_type, sent_at, is_read, recipient_username, source_type, source_id)",
        "values(#{title}, #{messageType}, #{sentAt}, #{read}, #{recipientUsername}, #{sourceType}, #{sourceId})"
    })
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(NotificationMessage message);

    @Update({
        "update notification_messages set is_read = 1",
        "where recipient_username = #{username} and is_read = 0"
    })
    int markAllReadByUsername(@Param("username") String username);
}
