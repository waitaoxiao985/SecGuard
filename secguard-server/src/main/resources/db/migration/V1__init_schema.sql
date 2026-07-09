-- ============================================================
-- SecGuard V1: 初始化数据库 Schema
-- 13 张核心表 + 默认管理员账号
-- ============================================================

-- 1. Agent 注册表
CREATE TABLE sg_agent (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    agent_key       VARCHAR(64)  NOT NULL,
    name            VARCHAR(128) NOT NULL,
    ip              VARCHAR(64)  NOT NULL,
    os              VARCHAR(32)  NOT NULL COMMENT 'windows/linux/macos',
    os_version      VARCHAR(64),
    hostname        VARCHAR(128),
    agent_version   VARCHAR(32),
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISCONNECTED/PENDING/DISABLED',
    last_keepalive  DATETIME,
    cpu_usage       DOUBLE,
    mem_usage       DOUBLE,
    pid             INT,
    uptime          BIGINT,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_agent_key (agent_key),
    INDEX idx_agent_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. 系统用户表
CREATE TABLE sg_sys_user (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(64)  NOT NULL,
    password_hash   VARCHAR(256) NOT NULL,
    role            VARCHAR(32)  NOT NULL DEFAULT 'viewer' COMMENT 'admin/operator/viewer',
    display_name    VARCHAR(128),
    email           VARCHAR(128),
    enabled         TINYINT(1)   NOT NULL DEFAULT 1,
    last_login_at   DATETIME,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. 检测规则表
CREATE TABLE sg_rule (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_id         INT          NOT NULL COMMENT 'Wazuh 风格规则编号',
    name            VARCHAR(256) NOT NULL,
    description     TEXT,
    level           INT          NOT NULL DEFAULT 5 COMMENT '严重等级 0-15',
    category        VARCHAR(32)  NOT NULL,
    conditions      JSON         NOT NULL COMMENT '匹配条件 JSON',
    mitre_tactic    VARCHAR(256) COMMENT 'MITRE 战术 ID, 逗号分隔',
    mitre_technique VARCHAR(256) COMMENT 'MITRE 技术 ID, 逗号分隔',
    pci_dss         VARCHAR(256) COMMENT 'PCI DSS 条款',
    enabled         TINYINT(1)   NOT NULL DEFAULT 1,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_rule_id (rule_id),
    INDEX idx_rule_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. 安全告警表
CREATE TABLE sg_alert (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    agent_id        BIGINT,
    rule_id         BIGINT,
    rule_name       VARCHAR(256),
    severity        VARCHAR(20)  NOT NULL COMMENT 'LOW/MEDIUM/HIGH/CRITICAL',
    category        VARCHAR(32)  NOT NULL,
    description     VARCHAR(1024),
    mitre_tactic    VARCHAR(256),
    mitre_technique VARCHAR(256),
    raw_event       JSON,
    status          VARCHAR(20)  NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/ACKNOWLEDGED/RESOLVED/FALSE_POSITIVE',
    source_ip       VARCHAR(64),
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_alert_agent (agent_id),
    INDEX idx_alert_severity (severity),
    INDEX idx_alert_created (created_at),
    INDEX idx_alert_status (status),
    CONSTRAINT fk_alert_agent FOREIGN KEY (agent_id) REFERENCES sg_agent(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. FIM 文件基线表
CREATE TABLE sg_fim_baseline (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    agent_id        BIGINT       NOT NULL,
    file_path       VARCHAR(1024) NOT NULL,
    sha256_hash     VARCHAR(64)  NOT NULL,
    file_size       BIGINT,
    permissions     VARCHAR(32),
    owner           VARCHAR(128),
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_fim_agent_path (agent_id, file_path(255)),
    CONSTRAINT fk_fim_baseline_agent FOREIGN KEY (agent_id) REFERENCES sg_agent(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. FIM 变更事件表
CREATE TABLE sg_fim_event (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    agent_id        BIGINT       NOT NULL,
    file_path       VARCHAR(1024) NOT NULL,
    event_type      VARCHAR(20)  NOT NULL COMMENT 'ADDED/MODIFIED/DELETED',
    sha256_hash     VARCHAR(64),
    previous_hash   VARCHAR(64),
    file_size       BIGINT,
    permissions     VARCHAR(32),
    owner           VARCHAR(128),
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_fim_event_agent (agent_id),
    INDEX idx_fim_event_created (created_at),
    CONSTRAINT fk_fim_event_agent FOREIGN KEY (agent_id) REFERENCES sg_agent(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. 主机系统信息表
CREATE TABLE sg_asset_system (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    agent_id        BIGINT       NOT NULL,
    hostname        VARCHAR(128),
    os              VARCHAR(64),
    os_version      VARCHAR(64),
    kernel          VARCHAR(64),
    cpu_model       VARCHAR(128),
    cpu_cores       INT,
    ram_total_mb    BIGINT,
    uptime_seconds  BIGINT,
    collected_at    DATETIME     NOT NULL,
    INDEX idx_asset_sys_agent (agent_id),
    CONSTRAINT fk_asset_sys_agent FOREIGN KEY (agent_id) REFERENCES sg_agent(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. 已安装软件清单表
CREATE TABLE sg_asset_software (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    agent_id        BIGINT       NOT NULL,
    name            VARCHAR(256) NOT NULL,
    version         VARCHAR(64),
    vendor          VARCHAR(128),
    format          VARCHAR(32) COMMENT 'deb/rpm/msi/exe',
    collected_at    DATETIME     NOT NULL,
    INDEX idx_asset_sw_agent (agent_id),
    INDEX idx_asset_sw_name (name),
    CONSTRAINT fk_asset_sw_agent FOREIGN KEY (agent_id) REFERENCES sg_agent(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. 开放端口表
CREATE TABLE sg_asset_port (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    agent_id        BIGINT       NOT NULL,
    protocol        VARCHAR(16)  NOT NULL COMMENT 'tcp/udp',
    local_port      INT          NOT NULL,
    state           VARCHAR(32) COMMENT 'LISTEN/ESTABLISHED',
    process_name    VARCHAR(128),
    process_pid     INT,
    collected_at    DATETIME     NOT NULL,
    INDEX idx_asset_port_agent (agent_id),
    CONSTRAINT fk_asset_port_agent FOREIGN KEY (agent_id) REFERENCES sg_agent(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. 网络接口表
CREATE TABLE sg_asset_network (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    agent_id        BIGINT       NOT NULL,
    interface_name  VARCHAR(64)  NOT NULL,
    mac_address     VARCHAR(32),
    ipv4            VARCHAR(64),
    ipv6            VARCHAR(128),
    gateway         VARCHAR(64),
    dns             VARCHAR(256),
    collected_at    DATETIME     NOT NULL,
    INDEX idx_asset_net_agent (agent_id),
    CONSTRAINT fk_asset_net_agent FOREIGN KEY (agent_id) REFERENCES sg_agent(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 11. CVE 漏洞库表
CREATE TABLE sg_cve_entry (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    cve_id          VARCHAR(32)  NOT NULL COMMENT '如 CVE-2024-12345',
    severity        VARCHAR(20)  NOT NULL,
    cvss_score      DECIMAL(3,1),
    description     TEXT,
    affected_product VARCHAR(256),
    affected_version VARCHAR(256),
    published_at    DATE,
    INDEX idx_cve_id (cve_id),
    INDEX idx_cve_product (affected_product)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 12. 漏洞匹配结果表
CREATE TABLE sg_vulnerability (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    agent_id        BIGINT       NOT NULL,
    cve_id          VARCHAR(32)  NOT NULL,
    software_name   VARCHAR(256),
    software_version VARCHAR(64),
    severity        VARCHAR(20),
    status          VARCHAR(20)  NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/PATCHED/IGNORED',
    detected_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_vuln_agent (agent_id),
    INDEX idx_vuln_cve (cve_id),
    CONSTRAINT fk_vuln_agent FOREIGN KEY (agent_id) REFERENCES sg_agent(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 13. Agent 配置下发表
CREATE TABLE sg_agent_config (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    agent_id        BIGINT       NOT NULL,
    log_paths       JSON         COMMENT '日志采集路径',
    fim_paths       JSON         COMMENT 'FIM 监控路径',
    log_interval    INT          DEFAULT 5,
    fim_interval    INT          DEFAULT 300,
    inventory_interval INT       DEFAULT 3600,
    heartbeat_interval INT       DEFAULT 30,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_agent_config (agent_id),
    CONSTRAINT fk_agent_config FOREIGN KEY (agent_id) REFERENCES sg_agent(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 种子数据：默认管理员账号
-- 用户名: admin  密码: SecGuard@2026
-- BCrypt hash 需要运行时生成，这里用占位符
-- 在 SecGuardApplication 启动时由 DataInitializer 插入
-- ============================================================
