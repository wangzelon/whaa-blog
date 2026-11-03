# WhaaBlog 本地开发

## Docker Compose 部署

仓库根目录的 Compose 配置会构建并启动 API、博客前台、管理后台，以及 MySQL、Redis 和兼容 MinIO S3 API 的 RustFS。首次运行前，在仓库根目录创建 `.env` 并填写独立凭据；`.env` 已被 Git 忽略：

```powershell
Copy-Item .env.example .env
```

启动全部服务：

```bash
docker compose up -d --build
docker compose ps
```

- 博客前台：<http://localhost:8081>
- 管理后台：<http://localhost:8082>（用户名 `admin`，密码为 `.env` 中的 `WHA_ADMIN_PASSWORD`）
- API / Swagger：<http://localhost:18080/swagger-ui.html>
- MinIO 控制台：<http://localhost:9001>
- 媒体对象使用独立的 `whaa_rustfs_data` 数据卷；原 `whaa_minio_data` 卷会保留
- MySQL 映射到宿主机的 `3307` 端口（本地覆盖配置）

停止服务但保留数据库和媒体数据：`docker compose down`。删除数据卷会永久删除本地数据库和媒体文件。

## 依赖

- JDK 17+
- Maven 3.9+
- Node.js 22+
- Docker（推荐，用于 MySQL / Redis / MinIO）

## 1. 配置本地凭据

复制 `.env.example` 为 `.env`，并为数据库、MinIO、管理员密码和 JWT 密钥填写本地专用值。`WHA_ADMIN_PASSWORD` 至少 12 个字符；`WHA_JWT_SECRET` 至少使用 32 字节的随机值。`.env` 已加入 Git 忽略规则，不要提交真实凭据。

```powershell
Copy-Item .env.example .env
```

## 2. 启动中间件

```bash
docker compose up -d
```

## 3. 启动后端 API

```bash
mvn -pl whaa-blog-api -am spring-boot:run -Dspring-boot.run.profiles=dev
```

- API: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui.html
- Atom: http://localhost:8080/feed/atom.xml

## 4. 启动前台

```bash
cd whaa-blog-web
npm install
npm run dev
```

访问 http://localhost:5173

## 5. 启动管理后台

```bash
cd whaa-blog-admin
npm install
npm run dev
```

访问 http://localhost:5174

## 管理员账号

| 字段 | 配置 |
|------|-----|
| 用户名 | `admin` |
| 密码 | `.env` 中的 `WHA_ADMIN_PASSWORD` |

首次启动会创建管理员账号，已存在的管理员密码会在启动时与 `WHA_ADMIN_PASSWORD` 对齐（见 `AdminPasswordInitializer`）。

## 主要 API 前缀

- 公开读接口：`GET /api/v1/**`
- 登录 / 注册：`POST /api/v1/auth/login`、`POST /api/v1/auth/register`
- 私信 REST API：`/api/v1/chat/**`（需登录）
- 私信 WebSocket：`/ws/chat`（需先通过已认证接口取得 30 秒有效、一次性连接票据；握手成功后才升级）
- 管理端：`/api/v1/admin/**`（需 `Authorization: Bearer <token>`）

新注册账号共享 `sys_user` 用户表，角色固定为 `USER`；后台账号角色为 `ADMIN`。普通用户可以和其他用户或管理员私聊，并且只能读取自己参与的会话。管理员可以查看所有会话；在未参与的用户私聊中只读，不能代替会话参与者发送消息，也不会改变他们的已读状态。消息支持最多 2000 字文本，以及每条最多 5 个图片或常见文档附件；单个附件最多 20 MB。聊天附件存放在私有对象存储桶中，只能由会话参与者或管理员下载。

管理员可在“用户管理”中搜索和分页查看普通用户，重置密码、禁用或重新启用账号。禁用会立即撤销旧访问/刷新令牌并关闭现有 WebSocket。删除用户会匿名化并释放原用户名，保留文章、私信和媒体等关联内容，避免连带删除历史数据；已注销账号不能恢复。

私信和附件元数据使用 MySQL 持久化，附件文件保存在 RustFS 私有桶，登录频控和短时 WebSocket 票据使用 Redis。未发送的附件上传会在 30 分钟后清理。在线管理员会收到所有会话的新消息事件。WebSocket 在线会话目前保存在单个 API 实例的内存中；多实例部署时需要增加 Redis Pub/Sub 或消息代理做跨实例投递，并在反向代理中启用 `/ws/chat` 的 WebSocket Upgrade 转发。

## 数据库

- 库名：`whaa_blog`
- 用户名：`whaa`；密码由 `.env` 中的 `MYSQL_PASSWORD` 提供
- 表结构由 Flyway 自动迁移（`whaa-blog-api/src/main/resources/db/migration`）
