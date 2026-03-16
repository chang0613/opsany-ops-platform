package com.opsany.replica.repository;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.opsany.replica.domain.AiConversation;

@Mapper
public interface AiConversationRepository {

    @Insert({
        "insert into ai_conversations(title, owner_username, created_at, updated_at)",
        "values(#{title}, #{ownerUsername}, #{createdAt}, #{updatedAt})"
    })
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AiConversation conversation);

    @Update({
        "update ai_conversations set title = #{title}, updated_at = #{updatedAt}",
        "where id = #{id} and owner_username = #{ownerUsername}"
    })
    int updateTitle(@Param("id") long id, @Param("ownerUsername") String ownerUsername, @Param("title") String title,
        @Param("updatedAt") java.time.LocalDateTime updatedAt);

    @Select({
        "select id, title, owner_username, created_at, updated_at",
        "from ai_conversations where id = #{id} limit 1"
    })
    AiConversation findById(@Param("id") long id);

    @Select({
        "select id, title, owner_username, created_at, updated_at",
        "from ai_conversations where owner_username = #{ownerUsername}",
        "order by updated_at desc limit 50"
    })
    List<AiConversation> findTop50ByOwnerOrderByUpdatedAtDesc(@Param("ownerUsername") String ownerUsername);
}

