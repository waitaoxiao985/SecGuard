-- ============================================================
-- SecGuard V3: 日志事件存储表
-- Agent 上报的原始日志事件，供后续规则引擎（W4）消费
-- ============================================================

CREATE TABLE sg_log_event (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    agent_id        BIGINT        NOT NULL,
    source          VARCHAR(1024) COMMENT '日志来源文件路径',
    format          VARCHAR(32)   COMMENT '日志格式: syslog/json/plain',
    raw_log         TEXT          NOT NULL COMMENT '原始日志内容',
    fields          JSON          COMMENT '解析后的结构化字段',
    category        VARCHAR(32)   COMMENT '预分类事件类别',
    event_time      DATETIME(3)   COMMENT '事件时间戳（毫秒精度）',
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_log_agent (agent_id),
    INDEX idx_log_category (category),
    INDEX idx_log_created (created_at),
    INDEX idx_log_event_time (event_time),
    CONSTRAINT fk_log_agent FOREIGN KEY (agent_id) REFERENCES sg_agent(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
