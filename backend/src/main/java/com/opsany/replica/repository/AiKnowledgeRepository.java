package com.opsany.replica.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.opsany.replica.domain.AiKnowledgeEntry;

@Mapper
public interface AiKnowledgeRepository {

    @Insert({
        "insert into ai_knowledge_entries(title, content, tags, source_type, source_id, owner_username, created_at, updated_at)",
        "values(#{title}, #{content}, #{tags}, #{sourceType}, #{sourceId}, #{ownerUsername}, #{createdAt}, #{updatedAt})"
    })
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AiKnowledgeEntry entry);

    @Update({
        "update ai_knowledge_entries set",
        "title = #{title},",
        "content = #{content},",
        "tags = #{tags},",
        "source_type = #{sourceType},",
        "source_id = #{sourceId},",
        "updated_at = #{updatedAt}",
        "where id = #{id} and owner_username = #{ownerUsername}"
    })
    int update(AiKnowledgeEntry entry);

    @Select({
        "select id, title, content, tags, source_type, source_id, owner_username, created_at, updated_at",
        "from ai_knowledge_entries where owner_username = #{ownerUsername}",
        "order by created_at desc limit 50"
    })
    List<AiKnowledgeEntry> findTop50ByOwnerOrderByCreatedAtDesc(@Param("ownerUsername") String ownerUsername);

    @Select({
        "select id, title, content, tags, source_type, source_id, owner_username, created_at, updated_at",
        "from ai_knowledge_entries where id = #{id} limit 1"
    })
    AiKnowledgeEntry findById(@Param("id") long id);

    @Delete("delete from ai_knowledge_entries where id = #{id} and owner_username = #{ownerUsername}")
    int deleteByIdAndOwner(@Param("id") long id, @Param("ownerUsername") String ownerUsername);
}

