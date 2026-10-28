# WhaaBlog API 概览

统一响应：

```json
{ "code": 0, "message": "success", "data": {}, "timestamp": 0 }
```

## 公开接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/posts` | 文章分页（`page`,`size`,`categoryId`,`tagId`） |
| GET | `/api/v1/posts/{slug}` | 文章详情 |
| GET | `/api/v1/posts/archive` | 归档时间轴 |
| GET | `/api/v1/posts/search?q=` | 全文搜索 |
| GET | `/api/v1/categories` | 分类列表 |
| GET | `/api/v1/tags?cloud=true` | 标签 / 词云 |
| GET | `/api/v1/pages/{slug}` | 页面（about / about-site / custom） |
| GET | `/api/v1/projects` | 项目列表 |
| GET | `/api/v1/links?random=true` | 友链 |
| GET | `/api/v1/albums` | 相册 |
| GET | `/api/v1/timeline/site` | 站点编年史 |
| GET | `/api/v1/site/config` | 公开站点配置 |
| GET/POST | `/api/v1/comments` | 评论列表 / 提交 |
| GET | `/feed/atom.xml` | Atom 订阅 |
| GET | `/sitemap.xml` | 站点地图 |

## 管理接口（JWT）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/auth/login` | 登录 |
| POST | `/api/v1/auth/refresh` | 刷新 Token |
| * | `/api/v1/admin/posts` | 文章 CRUD |
| * | `/api/v1/admin/*` | 分类、标签、页面、友链、项目、相册、编年史、评论审核、媒体、站点配置 |

详细字段见 Swagger UI。
