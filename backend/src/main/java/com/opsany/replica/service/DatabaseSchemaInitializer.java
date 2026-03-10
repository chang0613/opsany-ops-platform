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
