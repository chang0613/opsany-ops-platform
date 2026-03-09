package com.opsany.replica.domain;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "work_orders")
public class WorkOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String orderNo;

    @Column(nullable = false, length = 128)
    private String title;

    @Column(nullable = false, length = 32)
    private String type;

    @Column(nullable = false, length = 64)
    private String creatorUsername;

    @Column(nullable = false, length = 64)
    private String creatorDisplayName;

    @Column(nullable = false, length = 64)
    private String progress;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(nullable = false, length = 32)
    private String priority;

    @Column(nullable = false, length = 128)
    private String serviceName;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(nullable = false, length = 32)
    private String estimatedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
