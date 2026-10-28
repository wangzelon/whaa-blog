# WhaaBlog 本地开发

## 依赖

- JDK 17+
- Maven 3.9+
- Node.js 20+
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
- 登录：`POST /api/v1/auth/login`
- 管理端：`/api/v1/admin/**`（需 `Authorization: Bearer <token>`）

## 数据库

- 库名：`whaa_blog`
- 用户名：`whaa`；密码由 `.env` 中的 `MYSQL_PASSWORD` 提供
- 表结构由 Flyway 自动迁移（`whaa-blog-api/src/main/resources/db/migration`）
