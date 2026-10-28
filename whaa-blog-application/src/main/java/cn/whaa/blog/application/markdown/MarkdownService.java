package cn.whaa.blog.application.markdown;

import org.commonmark.Extension;
import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MarkdownService {

    private static final Pattern MORE_PATTERN = Pattern.compile("<!--\\s*more\\s*-->", Pattern.CASE_INSENSITIVE);

    private final Parser parser;
    private final HtmlRenderer renderer;

    public MarkdownService() {
        List<Extension> extensions = List.of(TablesExtension.create(), StrikethroughExtension.create(), AutolinkExtension.create());
        this.parser = Parser.builder().extensions(extensions).build();
        this.renderer = HtmlRenderer.builder().extensions(extensions).build();
    }

    public RenderResult render(String markdown) {
        if (markdown == null) {
            markdown = "";
        }
        String excerptMd = extractExcerptMarkdown(markdown);
        String html = toHtml(markdown);
        String excerptHtml = toHtml(excerptMd);
        return new RenderResult(html, excerptHtml, excerptMd.trim());
    }

    public String toHtml(String markdown) {
        Node document = parser.parse(markdown == null ? "" : markdown);
        return renderer.render(document);
    }

    public String extractExcerptMarkdown(String markdown) {
        Matcher matcher = MORE_PATTERN.matcher(markdown);
        if (matcher.find()) {
            return markdown.substring(0, matcher.start());
        }
        return markdown.length() > 400 ? markdown.substring(0, 400) + "..." : markdown;
    }

    public record RenderResult(String html, String excerptHtml, String excerptMarkdown) {
    }
}
