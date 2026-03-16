package com.opsany.replica.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.opsany.replica.domain.WorkOrderAttachmentConfig;

@Mapper
public interface WorkOrderAttachmentConfigRepository {

    int insert(WorkOrderAttachmentConfig config);

    int update(WorkOrderAttachmentConfig config);

    int deleteById(@Param("id") Long id);

    WorkOrderAttachmentConfig findById(@Param("id") Long id);

    WorkOrderAttachmentConfig findByConfigKey(@Param("configKey") String configKey);

    List<WorkOrderAttachmentConfig> findAll();

    long count();
}
