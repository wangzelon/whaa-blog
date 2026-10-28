CREATE TABLE sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(128),
    email VARCHAR(128),
    avatar VARCHAR(512),
    role VARCHAR(32) NOT NULL DEFAULT 'ADMIN',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    slug VARCHAR(128) NOT NULL UNIQUE,
    description VARCHAR(512),
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE tag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    slug VARCHAR(128) NOT NULL UNIQUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE post (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    slug VARCHAR(200) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(512),
    excerpt TEXT,
    content_md MEDIUMTEXT NOT NULL,
    content_html MEDIUMTEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    category_id BIGINT,
    author_id BIGINT NOT NULL,
    cover_url VARCHAR(512),
    view_count INT NOT NULL DEFAULT 0,
    pinned TINYINT(1) NOT NULL DEFAULT 0,
    allow_comment TINYINT(1) NOT NULL DEFAULT 1,
    hide_time TINYINT(1) NOT NULL DEFAULT 0,
    published_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FULLTEXT INDEX ft_post_search (title, description, excerpt, content_md),
    CONSTRAINT fk_post_category FOREIGN KEY (category_id) REFERENCES category(id),
    CONSTRAINT fk_post_author FOREIGN KEY (author_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE post_tag (
    post_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (post_id, tag_id),
    CONSTRAINT fk_post_tag_post FOREIGN KEY (post_id) REFERENCES post(id) ON DELETE CASCADE,
    CONSTRAINT fk_post_tag_tag FOREIGN KEY (tag_id) REFERENCES tag(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE page (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    slug VARCHAR(128) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    page_type VARCHAR(32) NOT NULL,
    icon VARCHAR(64),
    content_md MEDIUMTEXT NOT NULL,
    content_html MEDIUMTEXT,
    allow_comment TINYINT(1) NOT NULL DEFAULT 1,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE comment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    target_type VARCHAR(32) NOT NULL,
    target_id BIGINT NOT NULL,
    parent_id BIGINT,
    nickname VARCHAR(64) NOT NULL,
    email VARCHAR(128),
    website VARCHAR(512),
    content TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    ip_hash VARCHAR(64),
    user_agent VARCHAR(512),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_comment_target (target_type, target_id),
    INDEX idx_comment_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE friend_link (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    blog_name VARCHAR(128) NOT NULL,
    url VARCHAR(512) NOT NULL,
    avatar VARCHAR(512),
    description VARCHAR(512),
    color VARCHAR(32) DEFAULT '#888888',
    email VARCHAR(128),
    sort_order INT NOT NULL DEFAULT 0,
    visible TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE project (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(128) NOT NULL,
    description TEXT,
    url VARCHAR(512),
    icon VARCHAR(512),
    category_code VARCHAR(32) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    visible TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE album (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    slug VARCHAR(128) NOT NULL UNIQUE,
    title VARCHAR(128) NOT NULL,
    description VARCHAR(512),
    cover_url VARCHAR(512),
    visible TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE album_photo (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    album_id BIGINT NOT NULL,
    url VARCHAR(512) NOT NULL,
    caption VARCHAR(512),
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_album_photo_album FOREIGN KEY (album_id) REFERENCES album(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE site_timeline (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_date DATE NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE site_config (
    config_key VARCHAR(64) PRIMARY KEY,
    config_value JSON NOT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE media (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    object_key VARCHAR(512) NOT NULL,
    url VARCHAR(512) NOT NULL,
    original_name VARCHAR(255),
    mime_type VARCHAR(128),
    size_bytes BIGINT,
    uploader_id BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_media_uploader FOREIGN KEY (uploader_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO category (name, slug, description, sort_order) VALUES
('随笔', 'essay', '生活与思考', 1),
('技术', 'tech', '开发笔记', 2);

INSERT INTO tag (name, slug) VALUES
('开源', 'open-source'),
('前端', 'frontend'),
('Java', 'java');

INSERT INTO site_config (config_key, config_value) VALUES
('site', JSON_OBJECT(
    'title', 'WhaaBlog',
    'subtitle', 'All at sea.',
    'author', 'Whaa',
    'description', '一个类似云游君小站的个人博客',
    'url', 'http://localhost:5173',
    'icp', '',
    'mascot', JSON_OBJECT('enabled', false)
)),
('sidebar', JSON_OBJECT(
    'avatar', '/avatar.svg',
    'nav', JSON_ARRAY(
        JSON_OBJECT('label', '首页', 'path', '/'),
        JSON_OBJECT('label', '归档', 'path', '/archives'),
        JSON_OBJECT('label', '标签', 'path', '/tags'),
        JSON_OBJECT('label', '分类', 'path', '/categories'),
        JSON_OBJECT('label', '项目', 'path', '/projects'),
        JSON_OBJECT('label', '友链', 'path', '/links'),
        JSON_OBJECT('label', '相册', 'path', '/albums'),
        JSON_OBJECT('label', '关于', 'path', '/about')
    )
)),
('social', JSON_ARRAY(
    JSON_OBJECT('name', 'GitHub', 'icon', 'github', 'url', 'https://github.com')
)),
('theme', JSON_OBJECT('accent', '#0078e7', 'defaultMode', 'light')),
('comment', JSON_OBJECT('enabled', true, 'moderation', true)),
('links', JSON_OBJECT('random', true));

INSERT INTO page (slug, title, page_type, content_md, content_html, allow_comment) VALUES
('about', '关于我', 'ABOUT', '# 关于我\n\n见字如面，欢迎来访 WhaaBlog。', '<h1>关于我</h1><p>见字如面，欢迎来访 WhaaBlog。</p>', 1),
('about-site', '关于站点', 'ABOUT_SITE', '# 关于站点\n\nWhaaBlog 使用 Spring Boot + Vue 构建。', '<h1>关于站点</h1><p>WhaaBlog 使用 Spring Boot + Vue 构建。</p>', 0);

INSERT INTO site_timeline (event_date, title, content, sort_order) VALUES
('2026-01-01', 'WhaaBlog 立项', '开始搭建类云游君小站的博客系统。', 1);

INSERT INTO project (title, description, url, category_code, sort_order, visible) VALUES
('WhaaBlog', '本博客系统源码', 'https://github.com', 'tool', 1, 1);

INSERT INTO friend_link (name, blog_name, url, avatar, description, color, sort_order, visible) VALUES
('示例友链', '示例博客', 'https://example.com', '', '一句话描述', '#0078e7', 1, 1);
