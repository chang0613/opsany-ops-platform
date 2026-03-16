package com.opsany.replica.repository;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.opsany.replica.domain.AiMessage;

@Mapper
public interface AiMessageRepository {

    @Insert({
        "insert into ai_messages(conversation_id, role, content, created_at)",
        "values(#{conversationId}, #{role}, #{content}, #{createdAt})"
    })
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AiMessage message);

    @Select({
        "select id, conversation_id, role, content, created_at",
        "from ai_messages where conversation_id = #{conversationId}",
        "order by id asc limit 200"
    })
    List<AiMessage> findTop200ByConversationIdOrderByIdAsc(@Param("conversationId") long conversationId);
}

