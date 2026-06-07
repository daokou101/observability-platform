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
-- 告警规则表
-- ============================================
CREATE TABLE IF NOT EXISTS alert_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_name VARCHAR(128) NOT NULL COMMENT '规则名称',
    service VARCHAR(64) NOT NULL COMMENT '目标服务',
    metric VARCHAR(32) NOT NULL COMMENT '指标: error_count/error_rate/avg_duration',
    threshold DOUBLE NOT NULL COMMENT '告警阈值',
    window_minutes INT NOT NULL DEFAULT 5 COMMENT '统计窗口(分钟)',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    description VARCHAR(255) DEFAULT NULL COMMENT '描述',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警规则';

-- ============================================
-- 告警日志表
-- ============================================
CREATE TABLE IF NOT EXISTS alert_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_id BIGINT NOT NULL COMMENT '规则ID',
    rule_name VARCHAR(128) NOT NULL COMMENT '规则名称',
    service VARCHAR(64) NOT NULL COMMENT '目标服务',
    metric VARCHAR(32) NOT NULL COMMENT '指标',
    current_value DOUBLE NOT NULL COMMENT '当前值',
    threshold DOUBLE NOT NULL COMMENT '阈值',
    message VARCHAR(512) DEFAULT NULL COMMENT '告警消息',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_rule_id (rule_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警日志';

-- ============================================
-- 灰度路由规则表
-- ============================================
CREATE TABLE IF NOT EXISTS grayscale_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    service VARCHAR(64) NOT NULL COMMENT '目标服务',
    strategy VARCHAR(32) NOT NULL COMMENT '策略: user-hash/ip-range/header',
    rule_value VARCHAR(255) NOT NULL COMMENT '规则值: 0-50 / 192.168.1.0/24 / X-Canary:true',
    target_version VARCHAR(32) NOT NULL COMMENT '目标版本: canary/v2',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    description VARCHAR(255) DEFAULT NULL COMMENT '描述',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='灰度路由规则';
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

INSERT INTO alert_rule (rule_name, service, metric, threshold, window_minutes, enabled, description) VALUES
('平台服务-错误数告警', 'platform-service', 'error_count', 50, 5, 1, '5分钟内错误数超过50触发告警'),
('平台服务-平均耗时告警', 'platform-service', 'avg_duration', 3000, 5, 1, '5分钟内平均耗时超过3000ms触发告警')
ON DUPLICATE KEY UPDATE rule_name = VALUES(rule_name);

INSERT INTO grayscale_rule (service, strategy, rule_value, target_version, enabled, description) VALUES
('platform-service', 'header', 'X-Canary:true', 'canary', 1, '请求头 X-Canary=true 时路由到 canary 版本'),
('platform-service', 'user-hash', '0-10', 'canary', 1, '用户 ID 哈希前 10% 路由到 canary 版本')
ON DUPLICATE KEY UPDATE service = VALUES(service);
