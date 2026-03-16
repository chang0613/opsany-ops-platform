package com.opsany.replica.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.opsany.replica.domain.WorkOrderAttachment;

@Mapper
public interface WorkOrderAttachmentRepository {

    int insert(WorkOrderAttachment attachment);

    int deleteById(@Param("id") Long id);

    int deleteByOrderNo(@Param("orderNo") String orderNo);

    WorkOrderAttachment findById(@Param("id") Long id);

    List<WorkOrderAttachment> findByOrderNo(@Param("orderNo") String orderNo);

    long countByOrderNo(@Param("orderNo") String orderNo);

    long sumFileSizeByOrderNo(@Param("orderNo") String orderNo);
}
