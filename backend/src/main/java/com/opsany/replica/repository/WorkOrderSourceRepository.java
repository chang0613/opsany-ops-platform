package com.opsany.replica.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.opsany.replica.domain.WorkOrderSource;

@Mapper
public interface WorkOrderSourceRepository {

    int insert(WorkOrderSource source);

    int update(WorkOrderSource source);

    int deleteById(@Param("id") Long id);

    WorkOrderSource findById(@Param("id") Long id);

    WorkOrderSource findBySourceCode(@Param("sourceCode") String sourceCode);

    List<WorkOrderSource> findAll();

    List<WorkOrderSource> findEnabled();

    long count();
}
