# WhaaBlog

类 [云游君小站](https://www.yunyoujun.cn/) 的单作者博客：**Spring Boot 3 + Java 17 + MySQL 8**，前台与管理端为 **Vue 3 + Vite**，并使用 **Redis**、**MinIO**。

## 模块

| 目录 | 说明 |
|------|------|
| `whaa-blog-api` | HTTP 入口、Flyway、Swagger |
| `whaa-blog-application` | 业务服务 |
| `whaa-blog-infrastructure` | MyBatis-Plus、Security、Redis、MinIO |
| `whaa-blog-web` | 公开博客站点 |
| `whaa-blog-admin` | 管理后台 |

## 快速开始

见 [docs/README.md](docs/README.md)。

## 功能

- 文章 / 分类 / 标签 / 归档 / 搜索 / RSS
- 关于我、关于站点、自定义页面、站点编年史
- 项目展示、友链（随机排序）、相册
- 评论（审核、频控）
- 管理端 Markdown 编辑、媒体上传、站点 JSON 配置
