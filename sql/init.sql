-- 创建数据库
CREATE DATABASE IF NOT EXISTS observability DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE observability;

-- ============================================
-- 网关日志表（按月分区）
-- ============================================
CREATE TABLE IF NOT EXISTS gateway_log (
    id BIGINT AUTO_INCREMENT,
    trace_id VARCHAR(64) NOT NULL COMMENT '全链路追踪ID',
    service VARCHAR(64) NOT NULL COMMENT '服务名',
    path VARCHAR(256) NOT NULL COMMENT '请求路径',
    method VARCHAR(10) NOT NULL COMMENT '请求方法',
    status_code VARCHAR(5) NOT NULL COMMENT '状态码',
    duration BIGINT NOT NULL DEFAULT 0 COMMENT '响应耗时(ms)',
    request_time VARCHAR(32) DEFAULT NULL COMMENT '请求时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    PRIMARY KEY (id, create_time),
    INDEX idx_trace_id (trace_id),
    INDEX idx_service_time (service, create_time),
    INDEX idx_path_time (path, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
PARTITION BY RANGE (TO_DAYS(create_time)) (
    PARTITION p202506 VALUES LESS THAN (TO_DAYS('2025-07-01')),
    PARTITION p202512 VALUES LESS THAN (TO_DAYS('2026-01-01')),
    PARTITION p202606 VALUES LESS THAN (TO_DAYS('2026-07-01')),
    PARTITION p202612 VALUES LESS THAN (TO_DAYS('2027-01-01')),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);

-- ============================================
-- 采样规则表
-- ============================================
CREATE TABLE IF NOT EXISTS sampling_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    service VARCHAR(64) NOT NULL COMMENT '服务名',
    rate INT NOT NULL DEFAULT 100 COMMENT '采样率 0-100',
    description VARCHAR(255) DEFAULT NULL COMMENT '描述',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_service (service)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采样规则';

-- ============================================
-- 动态路由表
-- ============================================
CREATE TABLE IF NOT EXISTS dynamic_route (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    route_id VARCHAR(64) NOT NULL COMMENT '路由ID',
    uri VARCHAR(256) NOT NULL COMMENT '目标URI (lb://service 或 http://...)',
    predicates TEXT DEFAULT NULL COMMENT '路由断言(JSON)',
    filters TEXT DEFAULT NULL COMMENT '过滤器(JSON)',
    `order` INT NOT NULL DEFAULT 0 COMMENT '优先级',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    description VARCHAR(255) DEFAULT NULL COMMENT '描述',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_route_id (route_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动态路由';

-- ============================================
-- Sentinel 流控规则表
-- ============================================
CREATE TABLE IF NOT EXISTS sentinel_flow_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resource VARCHAR(255) NOT NULL COMMENT '资源名（接口路径）',
    limit_app VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '来源应用',
    grade INT NOT NULL DEFAULT 1 COMMENT '限流阈值类型：0-线程数，1-QPS',
    count DOUBLE NOT NULL DEFAULT 10 COMMENT '限流阈值',
    strategy INT NOT NULL DEFAULT 0 COMMENT '流控模式：0-直接，1-关联，2-链路',
    control_behavior INT NOT NULL DEFAULT 0 COMMENT '流控效果：0-快速失败，1-Warm Up，2-排队等待',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    description VARCHAR(255) DEFAULT NULL COMMENT '描述',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_resource (resource)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Sentinel 流控规则';

-- ============================================
-- 种子数据
-- ============================================
INSERT INTO sampling_rule (service, rate, description) VALUES
('platform-service', 100, '平台服务-全量采样'),
('gateway', 100, '网关-全量采样')
ON DUPLICATE KEY UPDATE service = VALUES(service);

INSERT INTO dynamic_route (route_id, uri, predicates, `order`, enabled, description) VALUES
('platform-service', 'lb://platform-service', '[{"name":"Path","args":{"_genkey_0":"/api/**"}}]', 1, 1, '平台服务路由')
ON DUPLICATE KEY UPDATE route_id = VALUES(route_id);

INSERT INTO sentinel_flow_rule (resource, limit_app, grade, count, strategy, control_behavior, enabled, description) VALUES
('/api/**', 'default', 1, 100, 0, 0, 1, '全局限流：QPS 阈值 100'),
('/api/logs/**', 'default', 1, 50, 0, 0, 1, '日志接口限流：QPS 阈值 50')
ON DUPLICATE KEY UPDATE resource = VALUES(resource);
