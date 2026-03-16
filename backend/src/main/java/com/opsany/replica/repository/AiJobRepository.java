package com.opsany.replica.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.opsany.replica.domain.AiJob;

@Mapper
public interface AiJobRepository {

    @Insert({
        "insert into ai_jobs(job_type, status, owner_username, input_json, result_json, created_at, updated_at)",
        "values(#{jobType}, #{status}, #{ownerUsername}, #{inputJson}, #{resultJson}, #{createdAt}, #{updatedAt})"
    })
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AiJob job);

    @Select({
        "select id, job_type, status, owner_username, input_json, result_json, created_at, updated_at",
        "from ai_jobs where id = #{id} limit 1"
    })
    AiJob findById(@Param("id") long id);

    @Select({
        "select id, job_type, status, owner_username, input_json, result_json, created_at, updated_at",
        "from ai_jobs where owner_username = #{ownerUsername}",
        "order by created_at desc limit 50"
    })
    List<AiJob> findTop50ByOwnerOrderByCreatedAtDesc(@Param("ownerUsername") String ownerUsername);

    @Update({
        "update ai_jobs set status = #{status}, result_json = #{resultJson}, updated_at = #{updatedAt}",
        "where id = #{id} and owner_username = #{ownerUsername}"
    })
    int updateResult(@Param("id") long id, @Param("ownerUsername") String ownerUsername, @Param("status") String status,
        @Param("resultJson") String resultJson, @Param("updatedAt") LocalDateTime updatedAt);
}

