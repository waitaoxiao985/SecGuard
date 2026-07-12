# SecGuard（鹰眼守卫）

> 基于 Wazuh 架构思想设计的轻量级 SIEM/XDR 主机安全监控平台

![Java](https://img.shields.io/badge/Java-21%2B-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-green)
![Vue](https://img.shields.io/badge/Vue-3.x-brightgreen)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue)
![Redis](https://img.shields.io/badge/Redis-7.2-red)
![License](https://img.shields.io/badge/License-MIT-yellow)

## 📋 项目简介

SecGuard 是一个自研的轻量级 SIEM（安全信息与事件管理）/ XDR（扩展检测与响应）平台，参考 Wazuh 架构设计，实现 Agent 端日志采集、文件完整性监控、主机资产清点，Server 端 YAML 规则引擎实时告警，配合 Vue 3 可视化 Dashboard 与 MITRE ATT&CK 攻击映射。

**项目定位**：网络安全方向学习项目，展示对 SIEM 核心机制的理解，适合安服/安全工程师岗位面试展示。

## ✨ 核心特性

- **Agent-Server 架构**：轻量级 Agent 部署在被监控主机，通过 HTTP API 与 Server 通信，支持心跳保活、配置下发
- **自研规则引擎**：YAML 定义检测规则，支持字段匹配/正则/数值比较/多级条件链，支持热重载不停服
- **文件完整性监控 (FIM)**：SHA-256 哈希基线 + 定时扫描 + Java WatchService 实时监控，增量变更检测
- **主机资产采集**：系统信息/已装软件/开放端口/网络接口/运行进程，支持增量同步
- **漏洞关联检测**：软件清单 × CVE 数据库 = 自动漏洞发现，高危漏洞自动告警
- **MITRE ATT&CK 映射**：每条规则关联战术和技术编号，Dashboard 展示攻击矩阵热力图
- **双认证体系**：Agent 端 agent_key 认证，Web 管理端 JWT Token 认证
- **实时告警推送**：WebSocket 实时推送告警到前端 Dashboard

## 🏗️ 技术栈

| 层级 | 技术 |
|------|------|
| **后端** | Java 21 + Spring Boot 3.3.5 + Spring Security 6 + Spring Data JPA |
| **数据库** | MySQL 8.0 + Flyway 数据库迁移 |
| **缓存/消息** | Redis 7.2 (Redis Stream 事件缓冲) |
| **前端** | Vue 3 + Vite + Element Plus + Tailwind CSS 3 + ECharts |
| **认证** | JWT (jjwt 0.12.6) + Agent Key Header |
| **构建** | Maven 3.9.9 多模块 |

## 📁 项目结构

```
SecGuard/
├── secguard-server/              # Spring Boot 服务端 (端口 8900)
│   ├── config/                   # 安全配置、CORS、数据初始化
│   ├── controller/               # REST API (Auth, Agent, LogEvent, FimEvent, Alert, Rule, Inventory, Vulnerability, Dashboard)
│   ├── service/                  # 业务逻辑 (AgentService, LogEventService, FimEventService, AlertService, AssetService, VulnerabilityService)
│   ├── entity/                   # JPA 实体 (Agent, Alert, LogEventEntity, FimEventEntity, FimBaselineEntity, SysUser, Asset*, CveEntry, Vulnerability)
│   ├── repository/               # 数据访问层
│   ├── security/                 # JWT + AgentKey 双认证过滤器
│   ├── dto/                      # 服务端专用 DTO
│   ├── engine/                   # 规则引擎（W4 核心亮点）
│   ├── websocket/                # WebSocket 实时推送
│   ├── scheduler/                # 定时任务（AgentHealthChecker 断线检测）
│   ├── consumer/                 # Redis Stream 消费者（W4）
│   └── resources/
│       ├── db/migration/         # Flyway 迁移脚本 (V1: 13表, V3: +日志事件表, V4: +基线增强)
│       └── rules/                # 内置 YAML 规则文件
│
├── secguard-agent/               # 轻量级 Agent (端口 8901)
│   ├── config/                   # AgentProperties 配置映射
│   ├── registration/             # Agent 注册（自动注册 + key 持久化）
│   ├── heartbeat/                # 心跳任务（系统指标上报 + 配置同步）
│   ├── collector/log/            # 日志采集（LogFileTailer + LogParser + LogCollector）
│   ├── collector/fim/            # FIM 文件完整性监控（FileHasher + FimBaseline + FimScanner + FimCollector）
│   ├── collector/inventory/      # 主机资产采集（SystemInfoCollector + SoftwareCollector + PortCollector + NetworkCollector + InventoryCollector）
│   ├── sender/                   # 事件上报（HTTP 批量 + 重试）
│   └── AgentStartupRunner.java   # 启动自动注册
│
├── secguard-common/              # 共享模块
│   ├── dto/                      # 数据传输对象
│   ├── enums/                    # 枚举（Severity, EventCategory...）
│   └── model/                    # 共享模型
│
├── secguard-rules/               # 规则定义（独立管理）
│   ├── base/                     # 内置基础规则
│   └── custom/                   # 用户自定义规则
│
├── secguard-web/                 # Vue 3 前端
│   ├── src/views/                # DashboardView, AgentsView, AlertsView, VulnerabilityView, FimView, InventoryView, RulesView, LoginView
│   ├── src/api/                  # API 客户端 (auth, dashboard, alert, fim, inventory, rule, vulnerability)
│   ├── src/layouts/              # MainLayout 侧边栏导航
│   ├── src/router/               # Vue Router 路由配置
│   └── src/stores/               # Pinia 状态管理 (auth)
├── secguard-cve/                 # CVE 漏洞数据
├── mvnw.cmd                      # Maven 包装脚本（隔离 Java 版本）
└── pom.xml                       # 父 POM
```

## 🗄️ 数据库设计 (14 张核心表)

| 表名 | 说明 |
|------|------|
| `sg_agent` | Agent 注册表（agent_key, 状态, 最后心跳） |
| `sg_log_event` | 日志事件存储表（Agent 上报的原始日志，含解析字段） |
| `sg_alert` | 安全告警表（规则命中、MITRE 映射、处置状态） |
| `sg_rule` | 检测规则表（YAML 条件、合规映射） |
| `sg_fim_baseline` | FIM 文件基线（SHA-256 哈希快照） |
| `sg_fim_event` | FIM 变更事件（added/modified/deleted） |
| `sg_asset_system` | 主机系统信息 |
| `sg_asset_software` | 已安装软件清单 |
| `sg_asset_port` | 开放端口 |
| `sg_asset_network` | 网络接口 |
| `sg_cve_entry` | CVE 漏洞库 |
| `sg_vulnerability` | 漏洞匹配结果 |
| `sg_sys_user` | 系统用户（admin/operator/viewer） |
| `sg_agent_config` | Agent 配置下发 |

## 🔌 API 端点

### Agent 通信

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/agents/register` | Agent 注册 | 公开 |
| POST | `/api/agents/heartbeat` | Agent 心跳 | X-Agent-Key |
| POST | `/api/events/logs` | 日志事件上报 | X-Agent-Key |
| POST | `/api/events/fim` | FIM 事件上报 | X-Agent-Key |
| POST | `/api/events/fim/baseline` | FIM 基线快照上报 | X-Agent-Key |
| POST | `/api/events/inventory` | 资产数据上报 | X-Agent-Key |

### 管理后台

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/auth/login` | 用户登录 | 公开 |
| GET | `/api/agents` | Agent 列表 | JWT |
| GET | `/api/agents/{id}` | Agent 详情 | JWT |
| PUT | `/api/agents/{id}/disable` | 禁用 Agent | JWT |
| PUT | `/api/agents/{id}/enable` | 启用 Agent | JWT |
| DELETE | `/api/agents/{id}` | 删除 Agent | JWT |
| GET | `/api/events/logs` | 日志事件查询（支持 agentId/category 过滤） | JWT |
| GET | `/api/events/stats` | 日志统计 | JWT |
| GET | `/api/events/fim` | FIM 事件查询（支持 agentId/eventType/时间范围/路径模糊过滤） | JWT |
| GET | `/api/events/fim/stats` | FIM 事件统计（按类型/Agent 分组、趋势、TOP 路径） | JWT |
| GET | `/api/events/fim/baseline` | FIM 基线查询（按 Agent 分页） | JWT |
| PUT | `/api/events/fim/baseline/{agentId}/reset` | 重置 Agent FIM 基线 | JWT |
| GET | `/api/alerts` | 告警列表（支持 severity/status/category 过滤） | JWT |
| GET | `/api/alerts/stats` | 告警统计 | JWT |
| GET | `/api/alerts/{id}` | 告警详情 | JWT |
| PUT | `/api/alerts/{id}/acknowledge` | 确认告警 | JWT |
| PUT | `/api/alerts/{id}/resolve` | 解决告警 | JWT |
| PUT | `/api/alerts/{id}/false-positive` | 标记误报 | JWT |
| GET | `/api/rules` | 已加载规则列表 | JWT |
| GET | `/api/rules/stats` | 规则统计 | JWT |
| POST | `/api/rules/reload` | 热重载规则 | JWT |
| GET | `/api/inventory/stats` | 资产统计摘要（Agent 数、软件/端口/网卡总数） | JWT |
| GET | `/api/inventory/summary/{agentId}` | Agent 资产概要（系统信息 + 各维度计数） | JWT |
| GET | `/api/inventory/system` | 查询最新系统信息 | JWT |
| GET | `/api/inventory/system/history` | 系统信息历史（分页） | JWT |
| GET | `/api/inventory/software` | 查询软件列表（分页） | JWT |
| GET | `/api/inventory/software/search` | 跨 Agent 软件搜索（名称模糊匹配） | JWT |
| GET | `/api/inventory/ports` | 查询端口列表（分页） | JWT |
| GET | `/api/inventory/ports/search` | 跨 Agent 端口号搜索 | JWT |
| GET | `/api/inventory/networks` | 查询网络接口（分页） | JWT |
| GET | `/api/vulnerabilities` | 漏洞列表（分页 + status/severity/agentId 过滤） | JWT |
| GET | `/api/vulnerabilities/agent/{agentId}` | Agent 漏洞列表 | JWT |
| POST | `/api/vulnerabilities/scan/{agentId}` | 扫描指定 Agent 漏洞 | JWT |
| POST | `/api/vulnerabilities/scan-all` | 全量漏洞扫描 | JWT |
| PUT | `/api/vulnerabilities/{id}/fix` | 标记漏洞为已修复 | JWT |
| PUT | `/api/vulnerabilities/{id}/ignore` | 标记漏洞为已忽略 | JWT |
| GET | `/api/vulnerabilities/stats` | 漏洞统计 | JWT |
| GET | `/api/cve` | CVE 知识库分页查询 | JWT |
| GET | `/api/cve/search` | CVE 产品搜索 | JWT |
| GET | `/api/cve/stats` | CVE 库统计 | JWT |
| GET | `/api/agents/stats` | Agent 统计（按状态分组） | JWT |
| GET | `/api/alerts/trend` | 24 小时告警趋势（按小时） | JWT |
| GET | `/api/alerts/mitre-stats` | MITRE ATT&CK 战术/技术分布统计 | JWT |

### WebSocket

| 路径 | 说明 |
|------|------|
| `/ws/alerts` | 实时告警推送 |
| `/ws/agent-status` | Agent 状态变化推送 |

## 🚀 快速开始

### 环境要求

- Java 21+
- Maven 3.9+
- MySQL 8.0+
- Redis 7.2+
- Node.js 18+ (前端)

### 1. 克隆项目

```bash
git clone https://github.com/waitaoxiao985/SecGuard.git
cd SecGuard
```

### 2. 创建数据库

```sql
CREATE DATABASE secguard DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3. 修改配置

编辑 `secguard-server/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/secguard?...
    username: root
    password: your_password
  data:
    redis:
      host: localhost
      port: 6379
```

### 4. 编译运行

```bash
# 编译所有模块
mvn clean install -DskipTests

# 启动 Server
java -jar secguard-server/target/secguard-server-1.0.0-SNAPSHOT.jar

# 启动 Agent（另一个终端）
java -jar secguard-agent/target/secguard-agent-1.0.0-SNAPSHOT.jar
```

Server 默认端口 `8900`，Agent 默认端口 `8901`。

### 5. 测试

```bash
# 登录获取 JWT
curl -X POST http://localhost:8900/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"SecGuard@2026"}'

# 注册 Agent（返回 agentKey）
curl -X POST http://localhost:8900/api/agents/register \
  -H "Content-Type: application/json" \
  -d '{"name":"test-agent","ip":"192.168.1.100","os":"linux","hostname":"ubuntu-lab"}'

# Agent 发送日志事件（用注册返回的 agentKey）
curl -X POST http://localhost:8900/api/events/logs \
  -H "Content-Type: application/json" \
  -H "X-Agent-Key: <your-agent-key>" \
  -d '[{"timestamp":"2026-07-10T09:30:01Z","rawLog":"Failed password for root","source":"/var/log/auth.log","format":"syslog","fields":{"program":"sshd","message":"Failed password"},"category":"AUTHENTICATION"}]'

# 查询日志事件（用 JWT）
curl http://localhost:8900/api/events/logs?page=0&size=10 -H "Authorization: Bearer <your-jwt>"
```

默认管理员账号：`admin` / `SecGuard@2026`

## 📖 开发计划 (10 周)

| 周次 | 目标 | 状态 |
|------|------|------|
| W1 | 项目骨架搭建 | ✅ 完成 |
| W2 | Agent 注册与通信 | ✅ 完成 |
| W3 | 日志采集（Agent 端） | ✅ 完成 |
| W4 | 规则引擎 + 告警 | ✅ 完成 |
| W5 | FIM 模块（Agent 端） | ✅ 完成 |
| W6 | FIM 模块（Server 端） | ✅ 完成 |
| W7 | 主机资产采集 | ✅ 完成 |
| W8 | 漏洞检测 + Dashboard 基础 | ✅ 完成 |
| W9 | Dashboard 完善 | ✅ 完成 |
| W10 | 收尾 + 文档 | ⏳ |

## 🎯 规则示例

```yaml
# SSH 暴力破解检测规则
- rule_id: 5710
  name: "SSH 登录失败"
  level: 5
  category: authentication
  description: "检测到 SSH 登录失败事件"
  mitre:
    tactic: ["TA0006"]        # Credential Access
    technique: ["T1110"]      # Brute Force
  pci_dss: ["10.2.4", "10.2.5"]
  conditions:
    field_match:
      - field: "program"
        operator: "equals"
        value: "sshd"
      - field: "message"
        operator: "regex"
        value: "Failed password for .* from (\\S+)"
    extract:
      - field: "source_ip"
        from: "message"
        regex: "from (\\S+)"
```

## 📄 License

MIT License
