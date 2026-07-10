-- ============================================================
-- SecGuard V4: FIM 基线管理增强
-- 添加唯一约束支持 agent+path upsert，增加 baseline_version 追踪
-- ============================================================

-- 添加唯一约束（agent + file_path 前缀 255）
ALTER TABLE sg_fim_baseline
    ADD UNIQUE KEY uk_fim_baseline_agent_path (agent_id, file_path(255));

-- 添加基线快照版本号（每次全量上报递增）
ALTER TABLE sg_fim_baseline
    ADD COLUMN baseline_version INT NOT NULL DEFAULT 1 COMMENT '基线快照版本';
