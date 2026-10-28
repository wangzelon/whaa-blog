package cn.whaa.blog.application.service;

import cn.whaa.blog.domain.enums.PostStatus;
import cn.whaa.blog.infrastructure.persistence.entity.PostEntity;
import cn.whaa.blog.infrastructure.persistence.mapper.PostMapper;
import cn.whaa.blog.infrastructure.persistence.mapper.SiteConfigMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedService {

    private final PostMapper postMapper;
    private final SiteConfigMapper siteConfigMapper;
    private final ObjectMapper objectMapper;

    public String buildAtomFeed() {
        String siteTitle = "WhaaBlog";
        String siteUrl = "http://localhost:5173";
        var siteConfig = siteConfigMapper.selectById("site");
        if (siteConfig != null) {
            try {
                JsonNode node = objectMapper.readTree(siteConfig.getConfigValue());
                siteTitle = node.path("title").asText(siteTitle);
                siteUrl = node.path("url").asText(siteUrl);
            } catch (Exception ignored) {
            }
        }
        List<PostEntity> posts = postMapper.selectList(new LambdaQueryWrapper<PostEntity>()
                .eq(PostEntity::getStatus, PostStatus.PUBLISHED.name())
                .orderByDesc(PostEntity::getPublishedAt)
                .last("LIMIT 50"));
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        sb.append("<feed xmlns=\"http://www.w3.org/2005/Atom\">\n");
        sb.append("<title>").append(escape(siteTitle)).append("</title>\n");
        sb.append("<link href=\"").append(escape(siteUrl)).append("\" />\n");
        sb.append("<updated>").append(formatDate(LocalDateTime.now())).append("</updated>\n");
        sb.append("<id>").append(escape(siteUrl)).append("</id>\n");
        for (PostEntity post : posts) {
            sb.append("<entry>\n");
            sb.append("<title>").append(escape(post.getTitle())).append("</title>\n");
            sb.append("<link href=\"").append(escape(siteUrl + "/posts/" + post.getSlug())).append("\" />\n");
            sb.append("<id>").append(escape(siteUrl + "/posts/" + post.getSlug())).append("</id>\n");
            LocalDateTime updated = post.getPublishedAt() != null ? post.getPublishedAt() : post.getUpdatedAt();
            sb.append("<updated>").append(formatDate(updated)).append("</updated>\n");
            sb.append("<summary>").append(escape(post.getExcerpt() == null ? "" : post.getExcerpt())).append("</summary>\n");
            sb.append("</entry>\n");
        }
        sb.append("</feed>");
        return sb.toString();
    }

    public String buildSitemap() {
        String siteUrl = "http://localhost:5173";
        var siteConfig = siteConfigMapper.selectById("site");
        if (siteConfig != null) {
            try {
                JsonNode node = objectMapper.readTree(siteConfig.getConfigValue());
                siteUrl = node.path("url").asText(siteUrl);
            } catch (Exception ignored) {
            }
        }
        List<PostEntity> posts = postMapper.selectList(new LambdaQueryWrapper<PostEntity>()
                .eq(PostEntity::getStatus, PostStatus.PUBLISHED.name()));
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        appendUrl(sb, siteUrl + "/", siteUrl);
        for (PostEntity post : posts) {
            appendUrl(sb, siteUrl + "/posts/" + post.getSlug(), siteUrl);
        }
        sb.append("</urlset>");
        return sb.toString();
    }

    private void appendUrl(StringBuilder sb, String loc, String siteUrl) {
        sb.append("<url><loc>").append(escape(loc)).append("</loc></url>\n");
    }

    private String formatDate(LocalDateTime time) {
        if (time == null) {
            time = LocalDateTime.now();
        }
        return time.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private String escape(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
