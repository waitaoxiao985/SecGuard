-- ============================================================
-- SecGuard V2: W2 Agent 通信完善
-- 为已有 Agent 补默认配置记录
-- ============================================================

-- 为所有已注册但没有配置记录的 Agent 插入默认配置
INSERT INTO sg_agent_config (agent_id, log_paths, fim_paths, log_interval, fim_interval, inventory_interval, heartbeat_interval)
SELECT a.id, '[]', '[]', 5, 300, 3600, 30
FROM sg_agent a
LEFT JOIN sg_agent_config c ON a.id = c.agent_id
WHERE c.id IS NULL;
