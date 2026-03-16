package com.opsany.replica.service;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@Order(0)
@RequiredArgsConstructor
public class DatabaseSchemaInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<String> createStatements = Arrays.asList(
            "create table if not exists app_users ("
                + "id bigint primary key auto_increment,"
                + "username varchar(64) not null unique,"
                + "display_name varchar(64) not null,"
                + "password_hash varchar(128) not null,"
                + "status varchar(32) not null,"
                + "created_at datetime not null,"
                + "enabled tinyint(1) not null default 1,"
                + "last_login_at datetime null"
                + ")",
            "create table if not exists app_roles ("
                + "role_code varchar(64) primary key,"
                + "role_name varchar(64) not null,"
                + "description varchar(255) null,"
                + "sort_no int not null default 0"
                + ")",
            "create table if not exists app_user_roles ("
                + "id bigint primary key auto_increment,"
                + "user_id bigint not null,"
                + "role_code varchar(64) not null,"
                + "unique key uk_app_user_roles (user_id, role_code)"
                + ")",
            "create table if not exists app_menus ("
                + "id bigint primary key auto_increment,"
                + "menu_code varchar(64) not null unique,"
                + "group_name varchar(64) not null,"
                + "group_sort_no int not null default 0,"
                + "label varchar(64) not null,"
                + "route varchar(128) not null,"
                + "icon varchar(32) null,"
                + "sort_no int not null default 0,"
                + "permission_code varchar(64) null,"
                + "visible tinyint(1) not null default 1"
                + ")",
            "create table if not exists app_role_menus ("
                + "id bigint primary key auto_increment,"
                + "role_code varchar(64) not null,"
                + "menu_code varchar(64) not null,"
                + "unique key uk_app_role_menus (role_code, menu_code)"
                + ")",
            "create table if not exists platform_navigation_groups ("
                + "id bigint primary key auto_increment,"
                + "group_code varchar(64) not null unique,"
                + "title varchar(64) not null,"
                + "sort_no int not null default 0"
                + ")",
            "create table if not exists platform_navigation_items ("
                + "id bigint primary key auto_increment,"
                + "item_code varchar(64) not null unique,"
                + "group_code varchar(64) not null,"
                + "name varchar(64) not null,"
                + "icon varchar(32) null,"
                + "creator_username varchar(64) not null,"
                + "creator_display_name varchar(64) not null,"
                + "link varchar(255) not null,"
                + "mobile_visible tinyint(1) not null default 0,"
                + "description varchar(255) null,"
                + "sort_no int not null default 0,"
                + "enabled tinyint(1) not null default 1"
                + ")",
            "create table if not exists app_user_navigation_favorites ("
                + "id bigint primary key auto_increment,"
                + "user_id bigint not null,"
                + "item_code varchar(64) not null,"
                + "sort_no int not null default 0,"
                + "unique key uk_app_user_navigation_favorites (user_id, item_code)"
                + ")",
            "create table if not exists platform_page_states ("
                + "id bigint primary key auto_increment,"
                + "platform_key varchar(64) not null,"
                + "page_key varchar(255) not null,"
                + "state_json longtext not null,"
                + "updated_by varchar(64) not null,"
                + "updated_at datetime not null,"
                + "unique key uk_platform_page_states (platform_key, page_key)"
                + ")",
            "create table if not exists work_order_processes ("
                + "id bigint primary key auto_increment,"
                + "process_code varchar(64) not null unique,"
                + "name varchar(128) not null,"
                + "category varchar(64) not null,"
                + "version int not null default 1,"
                + "status varchar(32) not null,"
                + "owner varchar(64) not null,"
                + "creator varchar(64) not null,"
                + "updater varchar(64) not null,"
                + "updated_at datetime not null,"
                + "description varchar(255) null,"
                + "definition_json text null"
                + ")",
            "create table if not exists work_order_process_nodes ("
                + "id bigint primary key auto_increment,"
                + "process_code varchar(64) not null,"
                + "node_code varchar(64) not null,"
                + "node_name varchar(64) not null,"
                + "node_type varchar(32) not null,"
                + "sort_no int not null default 0,"
                + "assignee_role varchar(64) null,"
                + "next_approve_node varchar(64) null,"
                + "next_reject_node varchar(64) null,"
                + "unique key uk_process_node (process_code, node_code)"
                + ")",
            "create table if not exists work_order_catalogs ("
                + "id bigint primary key auto_increment,"
                + "catalog_code varchar(64) not null unique,"
                + "name varchar(128) not null,"
                + "category varchar(64) not null,"
                + "type varchar(32) not null,"
                + "scope varchar(64) not null,"
                + "online tinyint(1) not null default 1,"
                + "process_code varchar(64) not null,"
                + "sla_name varchar(64) null,"
                + "owner_username varchar(64) not null,"
                + "owner_display_name varchar(64) not null,"
                + "description varchar(255) null,"
                + "sort_no int not null default 0,"
                + "created_at datetime not null,"
                + "updated_at datetime not null"
                + ")",
            "create table if not exists work_orders ("
                + "id bigint primary key auto_increment,"
                + "order_no varchar(32) not null unique,"
                + "title varchar(128) not null,"
                + "type varchar(32) not null,"
                + "creator_username varchar(64) not null,"
                + "creator_display_name varchar(64) not null,"
                + "progress varchar(64) not null,"
                + "status varchar(32) not null,"
                + "priority varchar(32) not null,"
                + "service_name varchar(128) not null,"
                + "description varchar(255) not null,"
                + "estimated_at varchar(32) not null,"
                + "created_at datetime not null,"
                + "updated_at datetime null,"
                + "process_code varchar(64) not null default 'STANDARD_REQUEST',"
                + "current_node_code varchar(64) null,"
                + "current_node_name varchar(64) null,"
                + "current_handler varchar(64) null"
                + ")",
            "create table if not exists work_order_histories ("
                + "id bigint primary key auto_increment,"
                + "order_no varchar(32) not null,"
                + "action varchar(32) not null,"
                + "from_status varchar(32) null,"
                + "to_status varchar(32) not null,"
                + "from_node_code varchar(64) null,"
                + "to_node_code varchar(64) null,"
                + "operator_username varchar(64) not null,"
                + "operator_display_name varchar(64) not null,"
                + "comment varchar(255) null,"
                + "created_at datetime not null"
                + ")",
            "create table if not exists task_records ("
                + "id bigint primary key auto_increment,"
                + "task_no varchar(32) not null unique,"
                + "title varchar(128) not null,"
                + "source varchar(64) not null,"
                + "ticket varchar(128) not null,"
                + "status varchar(32) not null,"
                + "assignee varchar(64) not null,"
                + "priority varchar(32) not null,"
                + "creator varchar(64) not null,"
                + "created_at datetime not null,"
                + "order_no varchar(32) null,"
                + "node_code varchar(64) null,"
                + "completed_at datetime null"
                + ")",
            "create table if not exists notification_messages ("
                + "id bigint primary key auto_increment,"
                + "title varchar(160) not null,"
                + "message_type varchar(64) not null,"
                + "sent_at datetime not null,"
                + "is_read tinyint(1) not null default 0,"
                + "recipient_username varchar(64) null,"
                + "source_type varchar(64) null,"
                + "source_id varchar(64) null"
                + ")",
            "create table if not exists login_audits ("
                + "id bigint primary key auto_increment,"
                + "user_id bigint not null,"
                + "username varchar(64) not null,"
                + "login_ip varchar(64) not null,"
                + "user_agent varchar(255) not null,"
                + "login_at datetime not null"
                + ")",
            "create table if not exists message_subscriptions ("
                + "id bigint primary key auto_increment,"
                + "username varchar(64) not null,"
                + "message_type varchar(64) not null,"
                + "source varchar(64) not null,"
                + "site_enabled tinyint(1) not null default 1,"
                + "sms_enabled tinyint(1) not null default 0,"
                + "mail_enabled tinyint(1) not null default 1,"
                + "wx_enabled tinyint(1) not null default 1,"
                + "ding_enabled tinyint(1) not null default 0,"
                + "updated_at datetime not null,"
                + "unique key uk_message_subscription (username, message_type, source)"
                + ")",
            "create table if not exists duty_groups ("
                + "id bigint primary key auto_increment,"
                + "name varchar(64) not null unique,"
                + "owner_username varchar(64) not null,"
                + "owner_display_name varchar(64) not null,"
                + "members int not null default 0,"
                + "coverage varchar(64) not null,"
                + "description varchar(255) null"
                + ")",
            "create table if not exists duty_schedules ("
                + "id bigint primary key auto_increment,"
                + "group_id bigint not null,"
                + "group_name varchar(64) not null,"
                + "duty_date date not null,"
                + "date_label varchar(64) not null,"
                + "shift_label varchar(32) not null,"
                + "shift_time varchar(64) not null,"
                + "owner_username varchar(64) not null,"
                + "owner_display_name varchar(64) not null,"
                + "status varchar(32) not null,"
                + "unique key uk_duty_group_date (group_id, duty_date)"
                + ")",
            "create table if not exists monitor_network_devices ("
                + "id bigint primary key auto_increment,"
                + "device_code varchar(64) not null unique,"
                + "name varchar(128) not null,"
                + "ip varchar(64) not null,"
                + "device_type varchar(64) not null,"
                + "vendor varchar(64) not null,"
                + "protocol varchar(32) not null,"
                + "snmp_community varchar(64) null,"
                + "snmp_version varchar(16) null,"
                + "ssh_username varchar(64) null,"
                + "port int not null default 161,"
                + "status varchar(32) not null,"
                + "last_collected_at datetime null,"
                + "description varchar(255) null,"
                + "created_at datetime not null,"
                + "updated_at datetime not null"
                + ")",
            "create table if not exists monitor_collection_templates ("
                + "id bigint primary key auto_increment,"
                + "template_code varchar(64) not null unique,"
                + "name varchar(128) not null,"
                + "device_type varchar(64) not null,"
                + "protocol varchar(32) not null,"
                + "metrics_json text not null,"
                + "interval_seconds int not null default 300,"
                + "enabled tinyint(1) not null default 1,"
                + "description varchar(255) null,"
                + "created_at datetime not null,"
                + "updated_at datetime not null"
                + ")",
            "create table if not exists monitor_metric_snapshots ("
                + "id bigint primary key auto_increment,"
                + "device_id bigint not null,"
                + "device_code varchar(64) not null,"
                + "metric_type varchar(64) not null,"
                + "metric_name varchar(128) not null,"
                + "metric_value varchar(255) not null,"
                + "collected_at datetime not null"
                + ")",
            "create table if not exists monitor_alert_rules ("
                + "id bigint primary key auto_increment,"
                + "rule_code varchar(64) not null,"
                + "name varchar(128) not null,"
                + "metric_type varchar(64),"
                + "condition_expr varchar(256),"
                + "threshold varchar(64),"
                + "severity varchar(32),"
                + "enabled tinyint(1) not null default 1,"
                + "notify_group varchar(128),"
                + "description varchar(256),"
                + "created_at datetime default now(),"
                + "updated_at datetime default now() on update now()"
                + ")",
            "create table if not exists monitor_service_probes ("
                + "id bigint primary key auto_increment,"
                + "probe_code varchar(64) not null,"
                + "name varchar(128) not null,"
                + "probe_type varchar(32),"
                + "target_url varchar(256),"
                + "protocol varchar(32),"
                + "interval_seconds int default 60,"
                + "timeout_ms int default 5000,"
                + "status varchar(32) default '运行中',"
                + "last_result varchar(64),"
                + "last_checked_at datetime,"
                + "description varchar(256),"
                + "created_at datetime default now(),"
                + "updated_at datetime default now() on update now()"
                + ")",
            "create table if not exists monitor_log_collectors ("
                + "id bigint primary key auto_increment,"
                + "collector_code varchar(64) not null,"
                + "name varchar(128) not null,"
                + "source_type varchar(32),"
                + "source_path varchar(256),"
                + "target_host varchar(128),"
                + "encoding varchar(32) default 'UTF-8',"
                + "status varchar(32) default '运行中',"
                + "lines_collected bigint default 0,"
                + "last_collected_at datetime,"
                + "description varchar(256),"
                + "created_at datetime default now(),"
                + "updated_at datetime default now() on update now()"
                + ")",
            "create table if not exists monitor_infra_servers ("
                + "id bigint primary key auto_increment,"
                + "server_code varchar(64) not null unique,"
                + "hostname varchar(128) not null,"
                + "ip varchar(64) not null,"
                + "model varchar(64) null,"
                + "cpu_usage varchar(16) null,"
                + "mem_usage varchar(16) null,"
                + "disk_usage varchar(16) null,"
                + "cpu_temp varchar(16) null,"
                + "collect_method varchar(32) null,"
                + "status varchar(32) not null,"
                + "last_collected_at datetime null,"
                + "description varchar(255) null,"
                + "created_at datetime not null,"
                + "updated_at datetime not null"
                + ")",
            "create table if not exists monitor_k8s_clusters ("
                + "id bigint primary key auto_increment,"
                + "cluster_code varchar(64) not null unique,"
                + "cluster_name varchar(128) not null,"
                + "version varchar(32) null,"
                + "node_count int not null default 0,"
                + "pod_count int not null default 0,"
                + "cpu_usage varchar(16) null,"
                + "mem_usage varchar(16) null,"
                + "status varchar(32) not null,"
                + "last_collected_at datetime null,"
                + "created_at datetime not null,"
                + "updated_at datetime not null"
                + ")",
            "create table if not exists monitor_k8s_pods ("
                + "id bigint primary key auto_increment,"
                + "pod_code varchar(64) not null unique,"
                + "cluster_code varchar(64) not null,"
                + "namespace varchar(64) not null,"
                + "pod_name varchar(128) not null,"
                + "node_name varchar(128) null,"
                + "cpu_usage varchar(16) null,"
                + "mem_usage varchar(16) null,"
                + "restart_count int not null default 0,"
                + "status varchar(32) not null,"
                + "created_at datetime not null"
                + ")",
            "create table if not exists monitor_middleware_instances ("
                + "id bigint primary key auto_increment,"
                + "instance_code varchar(64) not null unique,"
                + "name varchar(128) not null,"
                + "middleware_type varchar(64) not null,"
                + "version varchar(32) null,"
                + "host varchar(128) not null,"
                + "port int not null default 0,"
                + "cpu_usage varchar(16) null,"
                + "mem_usage varchar(16) null,"
                + "connect_count varchar(32) null,"
                + "qps varchar(32) null,"
                + "status varchar(32) not null,"
                + "last_collected_at datetime null,"
                + "created_at datetime not null,"
                + "updated_at datetime not null"
                + ")",
            "create table if not exists monitor_storage_nodes ("
                + "id bigint primary key auto_increment,"
                + "node_code varchar(64) not null unique,"
                + "node varchar(128) not null,"
                + "role varchar(32) not null,"
                + "used varchar(32) null,"
                + "total varchar(32) null,"
                + "write_rate varchar(32) null,"
                + "query_rate varchar(32) null,"
                + "status varchar(32) not null,"
                + "updated_at datetime not null"
                + ")",
            "create table if not exists monitor_archive_policies ("
                + "id bigint primary key auto_increment,"
                + "policy_code varchar(64) not null unique,"
                + "policy_name varchar(128) not null,"
                + "target_type varchar(64) not null,"
                + "hot_days int not null default 7,"
                + "warm_days int not null default 30,"
                + "cold_days int not null default 90,"
                + "compress tinyint(1) not null default 1,"
                + "status varchar(32) not null,"
                + "created_at datetime not null,"
                + "updated_at datetime not null"
                + ")",
            "create table if not exists monitor_archive_history ("
                + "id bigint primary key auto_increment,"
                + "policy_code varchar(64) not null,"
                + "policy varchar(128) not null,"
                + "data_size varchar(32) null,"
                + "duration int null,"
                + "status varchar(32) not null,"
                + "executed_at datetime not null"
                + ")",
            "create table if not exists monitor_notify_channels ("
                + "id bigint primary key auto_increment,"
                + "channel_code varchar(64) not null unique,"
                + "channel_name varchar(128) not null,"
                + "channel_type varchar(64) not null,"
                + "config varchar(255) null,"
                + "sent_today int not null default 0,"
                + "fail_today int not null default 0,"
                + "status varchar(32) not null,"
                + "created_at datetime not null,"
                + "updated_at datetime not null"
                + ")",
            "create table if not exists monitor_alert_events ("
                + "id bigint primary key auto_increment,"
                + "event_code varchar(64) not null unique,"
                + "rule_name varchar(128) not null,"
                + "source varchar(128) not null,"
                + "severity varchar(32) not null,"
                + "status varchar(32) not null,"
                + "handler varchar(64) null,"
                + "triggered_at datetime not null,"
                + "resolved_at datetime null"
                + ")",
            "create table if not exists monitor_oncall_schedules ("
                + "id bigint primary key auto_increment,"
                + "duty_date date not null,"
                + "weekday varchar(16) not null,"
                + "group_name varchar(64) not null,"
                + "person varchar(64) not null,"
                + "shift varchar(32) not null,"
                + "status varchar(32) not null"
                + ")",
            "create table if not exists monitor_response_records ("
                + "id bigint primary key auto_increment,"
                + "alert_name varchar(128) not null,"
                + "responder varchar(64) not null,"
                + "response_min int null,"
                + "resolve_min int null,"
                + "result varchar(32) not null,"
                + "triggered_at datetime not null"
                + ")",
            "create table if not exists monitor_dashboards ("
                + "id bigint primary key auto_increment,"
                + "dashboard_code varchar(64) not null unique,"
                + "name varchar(128) not null,"
                + "category varchar(64) not null,"
                + "charts int not null default 0,"
                + "creator varchar(64) not null,"
                + "shared tinyint(1) not null default 0,"
                + "visit_today int not null default 0,"
                + "updated_at datetime not null,"
                + "created_at datetime not null"
                + ")",
            "create table if not exists monitor_topology_items ("
                + "id bigint primary key auto_increment,"
                + "topology_code varchar(64) not null unique,"
                + "name varchar(128) not null,"
                + "type varchar(64) not null,"
                + "node_count int not null default 0,"
                + "link_count int not null default 0,"
                + "abnormal int not null default 0,"
                + "auto_refresh tinyint(1) not null default 0,"
                + "updated_at datetime not null,"
                + "created_at datetime not null"
                + ")",
            "create table if not exists monitor_topology_links ("
                + "id bigint primary key auto_increment,"
                + "topology_code varchar(64) not null,"
                + "source_node varchar(128) not null,"
                + "target_node varchar(128) not null,"
                + "link_type varchar(32) not null,"
                + "latency varchar(32) null,"
                + "error_rate varchar(16) null,"
                + "status varchar(32) not null"
                + ")",
            "create table if not exists monitor_bigscreens ("
                + "id bigint primary key auto_increment,"
                + "screen_code varchar(64) not null unique,"
                + "name varchar(128) not null,"
                + "resolution varchar(32) null,"
                + "charts int not null default 0,"
                + "auto_play tinyint(1) not null default 0,"
                + "shared tinyint(1) not null default 0,"
                + "visit_today int not null default 0,"
                + "updated_at datetime not null,"
                + "created_at datetime not null"
                + ")",
            "create table if not exists monitor_chart_templates ("
                + "id bigint primary key auto_increment,"
                + "template_code varchar(64) not null unique,"
                + "template_name varchar(128) not null,"
                + "chart_type varchar(64) not null,"
                + "category varchar(64) not null,"
                + "used_count int not null default 0,"
                + "created_at datetime not null,"
                + "updated_at datetime not null"
                + ")"
            ,
            "create table if not exists ai_conversations ("
                + "id bigint primary key auto_increment,"
                + "title varchar(128) not null,"
                + "owner_username varchar(64) not null,"
                + "created_at datetime not null,"
                + "updated_at datetime not null"
                + ")",
            "create table if not exists ai_messages ("
                + "id bigint primary key auto_increment,"
                + "conversation_id bigint not null,"
                + "role varchar(16) not null,"
                + "content text not null,"
                + "created_at datetime not null,"
                + "index idx_ai_messages_conversation (conversation_id)"
                + ")",
            "create table if not exists ai_jobs ("
                + "id bigint primary key auto_increment,"
                + "job_type varchar(64) not null,"
                + "status varchar(32) not null,"
                + "owner_username varchar(64) not null,"
                + "input_json text null,"
                + "result_json text null,"
                + "created_at datetime not null,"
                + "updated_at datetime not null,"
                + "index idx_ai_jobs_owner_created (owner_username, created_at)"
                + ")",
            "create table if not exists ai_knowledge_entries ("
                + "id bigint primary key auto_increment,"
                + "title varchar(255) not null,"
                + "content text not null,"
                + "tags varchar(255) null,"
                + "source_type varchar(64) null,"
                + "source_id varchar(128) null,"
                + "owner_username varchar(64) not null,"
                + "created_at datetime not null,"
                + "updated_at datetime not null,"
                + "index idx_ai_kb_owner_created (owner_username, created_at)"
                + ")"
        );

        for (String statement : createStatements) {
            jdbcTemplate.execute(statement);
        }

        ensureColumn("app_users", "enabled", "alter table app_users add column enabled tinyint(1) not null default 1");
        ensureColumn("app_users", "last_login_at", "alter table app_users add column last_login_at datetime null");

        ensureColumn("work_orders", "updated_at", "alter table work_orders add column updated_at datetime null");
        ensureColumn("work_orders", "process_code", "alter table work_orders add column process_code varchar(64) not null default 'STANDARD_REQUEST'");
        ensureColumn("work_orders", "current_node_code", "alter table work_orders add column current_node_code varchar(64) null");
        ensureColumn("work_orders", "current_node_name", "alter table work_orders add column current_node_name varchar(64) null");
        ensureColumn("work_orders", "current_handler", "alter table work_orders add column current_handler varchar(64) null");

        ensureColumn("task_records", "order_no", "alter table task_records add column order_no varchar(32) null");
        ensureColumn("task_records", "node_code", "alter table task_records add column node_code varchar(64) null");
        ensureColumn("task_records", "completed_at", "alter table task_records add column completed_at datetime null");

        ensureColumn("notification_messages", "recipient_username", "alter table notification_messages add column recipient_username varchar(64) null");
        ensureColumn("notification_messages", "source_type", "alter table notification_messages add column source_type varchar(64) null");
        ensureColumn("notification_messages", "source_id", "alter table notification_messages add column source_id varchar(64) null");
    }

    private void ensureColumn(String tableName, String columnName, String alterSql) throws SQLException {
        Connection connection = jdbcTemplate.getDataSource().getConnection();
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            if (columnExists(metaData, tableName, columnName)) {
                return;
            }
        } finally {
            connection.close();
        }
        jdbcTemplate.execute(alterSql);
    }

    private boolean columnExists(DatabaseMetaData metaData, String tableName, String columnName) throws SQLException {
        if (matches(metaData, tableName, columnName)) {
            return true;
        }
        if (matches(metaData, tableName.toUpperCase(), columnName.toUpperCase())) {
            return true;
        }
        return matches(metaData, tableName.toLowerCase(), columnName.toLowerCase());
    }

    private boolean matches(DatabaseMetaData metaData, String tableName, String columnName) throws SQLException {
        try (ResultSet resultSet = metaData.getColumns(null, null, tableName, columnName)) {
            return resultSet.next();
        }
    }
}
