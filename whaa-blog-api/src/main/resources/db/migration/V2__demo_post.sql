INSERT INTO post (slug, title, description, excerpt, content_md, content_html, status, category_id, author_id, pinned, allow_comment, published_at)
SELECT
    'welcome-whaablog',
    '欢迎来到 WhaaBlog',
    '类云游君小站的 Spring Boot 博客',
    'WhaaBlog 已就绪，开始写作吧。',
    '# 欢迎来到 WhaaBlog\n\n这是第一篇演示文章。\n\n<!-- more -->\n\n## 功能一览\n\n- 文章 / 标签 / 分类 / 归档\n- 项目 / 友链 / 相册\n- 评论与 RSS\n',
    '<h1>欢迎来到 WhaaBlog</h1><p>这是第一篇演示文章。</p>',
    'PUBLISHED',
    (SELECT id FROM category WHERE slug = 'essay' LIMIT 1),
    (SELECT id FROM sys_user WHERE username = 'admin' LIMIT 1),
    1,
    1,
    NOW()
WHERE EXISTS (SELECT 1 FROM sys_user WHERE username = 'admin')
  AND NOT EXISTS (SELECT 1 FROM post WHERE slug = 'welcome-whaablog');

INSERT INTO post_tag (post_id, tag_id)
SELECT p.id, t.id
FROM post p, tag t
WHERE p.slug = 'welcome-whaablog' AND t.slug = 'java'
  AND NOT EXISTS (
    SELECT 1 FROM post_tag pt WHERE pt.post_id = p.id AND pt.tag_id = t.id
);
