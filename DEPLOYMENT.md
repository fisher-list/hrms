# HRMS 部署指南

## 前提条件

- Docker Engine ≥ 20.10
- Docker Compose V2（`docker compose` 命令）
- 至少 2 GB 可用内存

---

## 快速部署（一键启动）

```bash
# 1. 进入项目根目录
cd /path/to/hrms

# 2. 创建环境变量文件
cp .env.example .env

# 3. 编辑 .env，填入实际密钥
#    ⚠️ HRMS_AES_KEY 必须恰好 32 字节
#    ⚠️ JWT_SECRET 必须 ≥ 32 字符
vim .env

# 4. 一键构建并启动
docker compose up -d --build

# 5. 查看日志
docker compose logs -f

# 6. 访问
#    前端: http://localhost
#    API:  http://localhost/api/
#    健康检查: http://localhost/api/actuator/health
```

---

## 常用命令

```bash
# 停止所有服务
docker compose down

# 停止并删除数据卷（⚠️ 会清除数据库数据）
docker compose down -v

# 重新构建某个服务
docker compose build app
docker compose up -d app

# 查看某个服务日志
docker compose logs -f app
docker compose logs -f db

# 进入容器调试
docker compose exec app sh
docker compose exec db psql -U hrms -d hrms
```

---

## 环境变量说明

| 变量 | 必填 | 说明 | 示例值 |
|------|------|------|--------|
| `HRMS_AES_KEY` | ✅ | AES-256 加密密钥，恰好 32 字节 | `12345678901234567890123456789012` |
| `JWT_SECRET` | ✅ | JWT 签名密钥，≥ 32 字符 | `my-super-secret-jwt-key-2024-hrms` |
| `SPRING_PROFILES_ACTIVE` | | Spring Profile | `pg`（默认） |
| `PG_DATABASE` | | 数据库名 | `hrms` |
| `PG_USERNAME` | | 数据库用户名 | `hrms` |
| `PG_PASSWORD` | | 数据库密码 | `hrms` |
| `PG_PORT` | | PG 宿主机端口 | `5432` |
| `APP_PORT` | | 后端宿主机端口 | `8080` |
| `FRONTEND_PORT` | | 前端宿主机端口 | `80` |
| `HRMS_CORS_ALLOWED_ORIGINS` | | CORS 白名单 | `http://localhost` |
| `SWAGGER_ENABLED` | | 启用 API 文档 | `false` |
| `HRMS_SNOWFLAKE_WORKER_ID` | | Snowflake Worker ID | `1` |
| `HRMS_SNOWFLAKE_DATACENTER_ID` | | Snowflake Datacenter ID | `1` |

---

## 架构概览

```
┌─────────────┐     ┌─────────────┐     ┌──────────────────┐
│   Browser   │────▶│  Frontend   │────▶│   Backend (app)  │
│             │     │  (Nginx:80) │     │  (Spring Boot    │
│             │     │             │     │   :8080)         │
└─────────────┘     └─────────────┘     └────────┬─────────┘
                                                  │
                                          ┌───────▼─────────┐
                                          │  PostgreSQL     │
                                          │  (db:5432)      │
                                          └─────────────────┘
```

- **Frontend**: Nginx 提供静态文件，`/api/` 反向代理到后端
- **Backend**: Spring Boot 应用，连接 PostgreSQL
- **Database**: PostgreSQL 16，数据持久化到 Docker Volume

---

## 常见问题

### Q: 启动报错 `HRMS_AES_KEY is not configured`

**原因**: 未设置 AES 密钥环境变量。

**解决**: 在 `.env` 中设置 `HRMS_AES_KEY`，必须恰好 32 个字符：
```bash
HRMS_AES_KEY=12345678901234567890123456789012
```

### Q: 启动报错 `JWT_SECRET` 相关异常

**原因**: JWT 密钥未设置或长度不足 32 字符。

**解决**: 在 `.env` 中设置足够长的密钥：
```bash
JWT_SECRET=my-super-secret-jwt-key-at-least-32-chars
```

### Q: 数据库连接失败 / app 启动时 db 还没就绪

**原因**: PostgreSQL 还在初始化。

**解决**: `docker-compose.yml` 已配置 `depends_on` + `healthcheck`，app 会等待 db 健康后再启动。如果仍然失败：
```bash
docker compose restart app
```

### Q: 前端页面空白 / API 请求 404

**原因**: Nginx 反向代理配置或 CORS 问题。

**解决**:
1. 确认 `HRMS_CORS_ALLOWED_ORIGINS` 包含前端实际访问地址
2. 检查 Nginx 日志：`docker compose logs frontend`

### Q: 端口冲突（5432 / 8080 / 80）

**原因**: 宿主机已有服务占用相同端口。

**解决**: 在 `.env` 中修改端口映射：
```bash
PG_PORT=15432
APP_PORT=18080
FRONTEND_PORT=8080
```

### Q: 如何查看/修改数据库数据？

```bash
# 进入 PostgreSQL 命令行
docker compose exec db psql -U hrms -d hrms

# 常用 SQL
\dt                          -- 列出所有表
SELECT * FROM sys_user;      -- 查询用户表
\q                           -- 退出
```

### Q: 如何备份数据库？

```bash
docker compose exec db pg_dump -U hrms hrms > backup_$(date +%Y%m%d).sql
```

### Q: 如何恢复数据库？

```bash
cat backup.sql | docker compose exec -T db psql -U hrms -d hrms
```

### Q: 如何仅启动后端 + 数据库（不用 Docker 部署前端）？

```bash
docker compose up -d db app
```

然后本地启动前端：
```bash
cd frontend
npm install
npm run dev
```

---

## 生产环境建议

1. **密钥管理**: 使用 Docker Secrets 或外部密钥管理服务，不要将密钥写在 `.env` 文件中
2. **HTTPS**: 在 Nginx 前加反向代理（如 Traefik / Caddy）处理 TLS
3. **日志**: 配置日志收集（ELK / Loki）
4. **监控**: 启用 Prometheus + Grafana 监控 `/actuator/health` 端点
5. **备份**: 定期备份 PostgreSQL 数据卷
6. **资源限制**: 在 `docker-compose.yml` 中添加 `deploy.resources.limits`
