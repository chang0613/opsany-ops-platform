-- 工单生命周期管理系统数据库表结构
-- 创建时间: 2026-03-16

-- ==========================================
-- 1. 工单编号规则配置表
-- ==========================================
CREATE TABLE IF NOT EXISTS work_order_no_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    rule_name VARCHAR(50) NOT NULL COMMENT '规则名称',
    prefix VARCHAR(20) NOT NULL DEFAULT 'WO' COMMENT '工单前缀',
    sequence_start INT NOT NULL DEFAULT 1 COMMENT '序列号起始值',
    sequence_current INT NOT NULL DEFAULT 1 COMMENT '当前序列号',
    sequence_length INT NOT NULL DEFAULT 6 COMMENT '序列号长度(补零)',
    date_format VARCHAR(20) DEFAULT 'YYYYMMDD' COMMENT '日期格式',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    description VARCHAR(200) DEFAULT NULL COMMENT '描述',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    creator_username VARCHAR(50) DEFAULT NULL COMMENT '创建人',
    UNIQUE KEY uk_rule_name (rule_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单编号规则配置表';

-- ==========================================
-- 2. 工单附件表
-- ==========================================
CREATE TABLE IF NOT EXISTS work_order_attachment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    order_no VARCHAR(50) NOT NULL COMMENT '工单编号',
    file_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_path VARCHAR(500) NOT NULL COMMENT '文件存储路径',
    file_size BIGINT NOT NULL COMMENT '文件大小(字节)',
    file_type VARCHAR(50) NOT NULL COMMENT '文件MIME类型',
    file_extension VARCHAR(10) NOT NULL COMMENT '文件扩展名',
    upload_username VARCHAR(50) NOT NULL COMMENT '上传人用户名',
    upload_display_name VARCHAR(100) NOT NULL COMMENT '上传人显示名称',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    INDEX idx_order_no (order_no),
    INDEX idx_upload_username (upload_username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单附件表';

-- ==========================================
-- 3. 工单附件配置表
-- ==========================================
CREATE TABLE IF NOT EXISTS work_order_attachment_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    config_key VARCHAR(50) NOT NULL COMMENT '配置键',
    allowed_extensions TEXT NOT NULL COMMENT '允许的文件扩展名(逗号分隔)',
    max_file_size MB INT NOT NULL DEFAULT 10 COMMENT '单文件最大大小(MB)',
    max_total_size MB INT NOT NULL DEFAULT 50 COMMENT '工单最大附件总大小(MB)',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单附件配置表';

-- ==========================================
-- 4. 工单状态配置表
-- ==========================================
CREATE TABLE IF NOT EXISTS work_order_status_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    status_code VARCHAR(20) NOT NULL COMMENT '状态编码',
    status_name VARCHAR(50) NOT NULL COMMENT '状态名称',
    status_type VARCHAR(20) NOT NULL COMMENT '状态类型(OPEN/PROCESSING/CLOSED)',
    color VARCHAR(20) DEFAULT '#1890FF' COMMENT '状态颜色',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    is_initial TINYINT NOT NULL DEFAULT 0 COMMENT '是否为初始状态',
    is_final TINYINT NOT NULL DEFAULT 0 COMMENT '是否为终态',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    description VARCHAR(200) DEFAULT NULL COMMENT '描述',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_status_code (status_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单状态配置表';

-- ==========================================
-- 5. 工单状态流转权限配置表
-- ==========================================
CREATE TABLE IF NOT EXISTS work_order_transition_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    from_status VARCHAR(20) NOT NULL COMMENT '源状态',
    to_status VARCHAR(20) NOT NULL COMMENT '目标状态',
    role_code VARCHAR(50) NOT NULL COMMENT '角色编码',
    allow_creator TINYINT NOT NULL DEFAULT 0 COMMENT '是否允许创建人',
    allow_handler TINYINT NOT NULL DEFAULT 1 COMMENT '是否允许处理人',
    allow_admin TINYINT NOT NULL DEFAULT 1 COMMENT '是否允许管理员',
    is_auto_transition TINYINT NOT NULL DEFAULT 0 COMMENT '是否自动流转',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_from_status (from_status),
    INDEX idx_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单状态流转权限配置表';

-- ==========================================
-- 6. 工单处理日志表
-- ==========================================
CREATE TABLE IF NOT EXISTS work_order处理_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    order_no VARCHAR(50) NOT NULL COMMENT '工单编号',
    log_type VARCHAR(20) NOT NULL COMMENT '日志类型(PROCESS/COMMENT/SOLUTION)',
    content LONGTEXT NOT NULL COMMENT '日志内容(富文本)',
    operator_username VARCHAR(50) NOT NULL COMMENT '操作人用户名',
    operator_display_name VARCHAR(100) NOT NULL COMMENT '操作人显示名称',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_order_no (order_no),
    INDEX idx_log_type (log_type),
    INDEX idx_operator_username (operator_username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单处理日志表';

-- ==========================================
-- 7. 工单满意度评价表
-- ==========================================
CREATE TABLE IF NOT EXISTS work_order_satisfaction (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    order_no VARCHAR(50) NOT NULL COMMENT '工单编号',
    score INT NOT NULL COMMENT '评分(1-5)',
    score_label VARCHAR(20) NOT NULL COMMENT '评分标签(不满意/一般/满意/非常满意/非常满意+)',
    comment TEXT DEFAULT NULL COMMENT '评价备注',
    evaluator_username VARCHAR(50) NOT NULL COMMENT '评价人用户名',
    evaluator_display_name VARCHAR(100) NOT NULL COMMENT '评价人显示名称',
    evaluated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评价时间',
    UNIQUE KEY uk_order_no (order_no),
    INDEX idx_evaluator_username (evaluator_username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单满意度评价表';

-- ==========================================
-- 8. 工单满意度配置表
-- ==========================================
CREATE TABLE IF NOT EXISTS work_order_satisfaction_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    score INT NOT NULL COMMENT '评分',
    score_label VARCHAR(20) NOT NULL COMMENT '评分标签',
    color VARCHAR(20) DEFAULT '#FF4D4F' COMMENT '标签颜色',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    UNIQUE KEY uk_score (score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单满意度配置表';

-- ==========================================
-- 9. 工单催办记录表
-- ==========================================
CREATE TABLE IF NOT EXISTS work_order_reminder (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    order_no VARCHAR(50) NOT NULL COMMENT '工单编号',
    reminder_type VARCHAR(20) NOT NULL DEFAULT 'URGE' COMMENT '催办类型(URGE/REMIND)',
    reminder_username VARCHAR(50) NOT NULL COMMENT '催办人用户名',
    reminder_display_name VARCHAR(100) NOT NULL COMMENT '催办人显示名称',
    reminder_content VARCHAR(500) DEFAULT NULL COMMENT '催办内容',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '催办时间',
    INDEX idx_order_no (order_no),
    INDEX idx_reminder_username (reminder_username),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单催办记录表';

-- ==========================================
-- 10. 工单催办频率配置表
-- ==========================================
CREATE TABLE IF NOT EXISTS work_order_reminder_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    config_key VARCHAR(50) NOT NULL COMMENT '配置键',
    min_interval_minutes INT NOT NULL DEFAULT 60 COMMENT '最小催办间隔(分钟)',
    max_daily_reminders INT NOT NULL DEFAULT 3 COMMENT '每日最大催办次数',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    description VARCHAR(200) DEFAULT NULL COMMENT '描述',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单催办频率配置表';

-- ==========================================
-- 11. 工单合并记录表
-- ==========================================
CREATE TABLE IF NOT EXISTS work_order_merge (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    master_order_no VARCHAR(50) NOT NULL COMMENT '主工单编号',
    merged_order_no VARCHAR(50) NOT NULL COMMENT '被合并工单编号',
    merge_reason VARCHAR(500) DEFAULT NULL COMMENT '合并原因',
    operator_username VARCHAR(50) NOT NULL COMMENT '操作人用户名',
    operator_display_name VARCHAR(100) NOT NULL COMMENT '操作人显示名称',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '合并时间',
    UNIQUE KEY uk_merged_order_no (merged_order_no),
    INDEX idx_master_order_no (master_order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单合并记录表';

-- ==========================================
-- 12. 工单拆分记录表
-- ==========================================
CREATE TABLE IF NOT EXISTS work_order_split (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    source_order_no VARCHAR(50) NOT NULL COMMENT '源工单编号',
    new_order_no VARCHAR(50) NOT NULL COMMENT '新工单编号',
    split_content LONGTEXT NOT NULL COMMENT '拆分内容',
    split_reason VARCHAR(500) DEFAULT NULL COMMENT '拆分原因',
    operator_username VARCHAR(50) NOT NULL COMMENT '操作人用户名',
    operator_display_name VARCHAR(100) NOT NULL COMMENT '操作人显示名称',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '拆分时间',
    INDEX idx_source_order_no (source_order_no),
    INDEX idx_new_order_no (new_order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单拆分记录表';

-- ==========================================
-- 13. 工单自动关闭配置表
-- ==========================================
CREATE TABLE IF NOT EXISTS work_order_auto_close_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    config_key VARCHAR(50) NOT NULL COMMENT '配置键',
    status VARCHAR(20) NOT NULL COMMENT '触发状态',
    days_after INT NOT NULL DEFAULT 7 COMMENT '多少天后自动关闭',
    action VARCHAR(20) NOT NULL DEFAULT 'CLOSE' COMMENT '执行动作(CLOSE/ARCHIVE)',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    description VARCHAR(200) DEFAULT NULL COMMENT '描述',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_config_key_status (config_key, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单自动关闭配置表';

-- ==========================================
-- 14. 工单创建来源表
-- ==========================================
CREATE TABLE IF NOT EXISTS work_order_source (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    source_code VARCHAR(20) NOT NULL COMMENT '来源编码(MANUAL/EMAIL/MONITOR/API)',
    source_name VARCHAR(50) NOT NULL COMMENT '来源名称',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    description VARCHAR(200) DEFAULT NULL COMMENT '描述',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_source_code (source_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单创建来源表';

-- ==========================================
-- 15. 工单CMDB资源关联表
-- ==========================================
CREATE TABLE IF NOT EXISTS work_order_resource (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    order_no VARCHAR(50) NOT NULL COMMENT '工单编号',
    resource_type VARCHAR(50) NOT NULL COMMENT '资源类型(SERVER/NETWORK/DB等)',
    resource_id VARCHAR(100) NOT NULL COMMENT '资源ID',
    resource_name VARCHAR(200) NOT NULL COMMENT '资源名称',
    resource_info JSON DEFAULT NULL COMMENT '资源详细信息',
    match_method VARCHAR(20) DEFAULT 'MANUAL' COMMENT '匹配方式(MANUAL/AUTO)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_order_no (order_no),
    INDEX idx_resource_type (resource_type),
    INDEX idx_resource_id (resource_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单CMDB资源关联表';

-- ==========================================
-- 16. 工单扩展信息表(存储更多字段)
-- ==========================================
CREATE TABLE IF NOT EXISTS work_order_ext (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    order_no VARCHAR(50) NOT NULL COMMENT '工单编号',
    source VARCHAR(20) DEFAULT 'MANUAL' COMMENT '创建来源',
    solution LONGTEXT DEFAULT NULL COMMENT '解决方案',
    is_requester_confirmed TINYINT NOT NULL DEFAULT 0 COMMENT '是否已提交人确认',
    requester_confirmed_at DATETIME DEFAULT NULL COMMENT '提交人确认时间',
    is_merged TINYINT NOT NULL DEFAULT 0 COMMENT '是否已合并',
    is_split TINYINT NOT NULL DEFAULT 0 COMMENT '是否已拆分',
    merged_to_order_no VARCHAR(50) DEFAULT NULL COMMENT '合并到的工单编号',
    original_order_no VARCHAR(50) DEFAULT NULL COMMENT '拆分前的原工单编号',
    close_reason VARCHAR(200) DEFAULT NULL COMMENT '关闭原因',
    reject_reason VARCHAR(500) DEFAULT NULL COMMENT '驳回原因',
    auto_close_at DATETIME DEFAULT NULL COMMENT '计划自动关闭时间',
    last_reminder_at DATETIME DEFAULT NULL COMMENT '最后催办时间',
    reminder_count INT NOT NULL DEFAULT 0 COMMENT '催办次数',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单扩展信息表';

-- ==========================================
-- 初始化默认数据
-- ==========================================

-- 初始化工单编号规则
INSERT INTO work_order_no_rule (rule_name, prefix, sequence_start, sequence_current, sequence_length, date_format, enabled, description)
VALUES ('default', 'WO', 1, 1, 6, 'YYYYMMDD', 1, '默认工单编号规则')
ON DUPLICATE KEY UPDATE prefix = VALUES(prefix);

-- 初始化工单创建来源
INSERT INTO work_order_source (source_code, source_name, sort_order, enabled, description) VALUES
('MANUAL', '手工录入', 1, 1, '手工创建工单'),
('EMAIL', '邮件导入', 2, 1, '通过邮件系统导入'),
('MONITOR', '监控系统触发', 3, 1, '由监控系统自动创建'),
('API', 'API接口调用', 4, 1, '通过API接口创建')
ON DUPLICATE KEY UPDATE source_name = VALUES(source_name);

-- 初始化工单状态配置
INSERT INTO work_order_status_config (status_code, status_name, status_type, color, sort_order, is_initial, is_final, enabled, description) VALUES
('DRAFT', '草稿', 'OPEN', '#8C8C8F', 1, 1, 0, 1, '工单草稿状态'),
('PENDING', '待受理', 'OPEN', '#FAAD14', 2, 0, 0, 1, '待客服受理'),
('PROCESSING', '处理中', 'PROCESSING', '#1890FF', 3, 0, 0, 1, '处理中'),
('PENDING_CONFIRM', '待确认', 'PROCESSING', '#722ED1', 4, 0, 0, 1, '待提交人确认'),
('CLOSED', '已关闭', 'CLOSED', '#52C41A', 5, 0, 1, 1, '已关闭'),
('ARCHIVED', '已归档', 'CLOSED', '#D9D9D9', 6, 0, 1, 1, '已归档'),
('REJECTED', '已驳回', 'OPEN', '#F5222D', 7, 0, 0, 1, '被管理员驳回')
ON DUPLICATE KEY UPDATE status_name = VALUES(status_name);

-- 初始化工单满意度配置
INSERT INTO work_order_satisfaction_config (score, score_label, color, sort_order, enabled) VALUES
(1, '非常不满意', '#F5222D', 1, 1),
(2, '不满意', '#FA8C16', 2, 1),
(3, '一般', '#FADB14', 3, 1),
(4, '满意', '#52C41A', 4, 1),
(5, '非常满意', '#1890FF', 5, 1)
ON DUPLICATE KEY UPDATE score_label = VALUES(score_label);

-- 初始化工单附件默认配置
INSERT INTO work_order_attachment_config (config_key, allowed_extensions, max_file_size_mb, max_total_size_mb, enabled)
VALUES ('default', 'jpg,jpeg,png,gif,pdf,doc,docx,xls,xlsx,zip,rar,txt', 10, 50, 1)
ON DUPLICATE KEY UPDATE allowed_extensions = VALUES(allowed_extensions);

-- 初始化催办频率配置
INSERT INTO work_order_reminder_config (config_key, min_interval_minutes, max_daily_reminders, enabled, description)
VALUES ('default', 60, 3, 1, '默认催办频率配置:每小时最多催办1次,每天最多3次')
ON DUPLICATE KEY UPDATE min_interval_minutes = VALUES(min_interval_minutes);

-- 初始化自动关闭配置
INSERT INTO work_order_auto_close_config (config_key, status, days_after, action, enabled, description)
VALUES ('pending_confirm_close', 'PENDING_CONFIRM', 3, 'CLOSE', 1, '待确认状态3天后自动关闭'),
('closed_archive', 'CLOSED', 30, 'ARCHIVE', 1, '已关闭状态30天后自动归档')
ON DUPLICATE KEY UPDATE days_after = VALUES(days_after);

-- 初始化状态流转权限(默认配置)
INSERT INTO work_order_transition_permission (from_status, to_status, role_code, allow_creator, allow_handler, allow_admin, is_auto_transition) VALUES
('DRAFT', 'PENDING', 'PLATFORM_ADMIN', 0, 0, 1, 0),
('PENDING', 'PROCESSING', 'PLATFORM_ADMIN', 0, 1, 1, 0),
('PENDING', 'REJECTED', 'PLATFORM_ADMIN', 0, 1, 1, 0),
('PROCESSING', 'PENDING_CONFIRM', 'PLATFORM_ADMIN', 0, 1, 1, 0),
('PROCESSING', 'REJECTED', 'PLATFORM_ADMIN', 0, 1, 1, 0),
('PENDING_CONFIRM', 'CLOSED', 'PLATFORM_ADMIN', 1, 0, 1, 0),
('PENDING_CONFIRM', 'PROCESSING', 'PLATFORM_ADMIN', 1, 0, 1, 0),
('REJECTED', 'PENDING', 'PLATFORM_ADMIN', 1, 0, 1, 0)
ON DUPLICATE KEY UPDATE allow_creator = VALUES(allow_creator);
