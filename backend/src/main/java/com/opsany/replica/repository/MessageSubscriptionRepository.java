package com.opsany.replica.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.opsany.replica.domain.MessageSubscription;

@Mapper
public interface MessageSubscriptionRepository {

    @Select({
        "select id, username, message_type, source, site_enabled, sms_enabled, mail_enabled, wx_enabled,",
        "ding_enabled, updated_at",
        "from message_subscriptions where username = #{username} order by id asc"
    })
    List<MessageSubscription> findByUsername(@Param("username") String username);

    @Select({
        "select id, username, message_type, source, site_enabled, sms_enabled, mail_enabled, wx_enabled,",
        "ding_enabled, updated_at",
        "from message_subscriptions where username = #{username} and message_type = #{messageType} and source = #{source} limit 1"
    })
    MessageSubscription findOne(
        @Param("username") String username,
        @Param("messageType") String messageType,
        @Param("source") String source
    );

    @Insert({
        "insert into message_subscriptions(",
        "username, message_type, source, site_enabled, sms_enabled, mail_enabled, wx_enabled, ding_enabled, updated_at",
        ") values (",
        "#{username}, #{messageType}, #{source}, #{siteEnabled}, #{smsEnabled}, #{mailEnabled}, #{wxEnabled},",
        "#{dingEnabled}, #{updatedAt}",
        ")"
    })
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(MessageSubscription subscription);

    @Update({
        "update message_subscriptions set site_enabled = #{siteEnabled}, sms_enabled = #{smsEnabled},",
        "mail_enabled = #{mailEnabled}, wx_enabled = #{wxEnabled}, ding_enabled = #{dingEnabled},",
        "updated_at = #{updatedAt}",
        "where id = #{id}"
    })
    int update(MessageSubscription subscription);

    @Update("update message_subscriptions set updated_at = #{updatedAt} where username = #{username}")
    int touchAllByUsername(@Param("username") String username, @Param("updatedAt") LocalDateTime updatedAt);
}
