package cn.whaa.blog.api.web;

import cn.whaa.blog.application.service.FeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    @GetMapping(value = "/feed/atom.xml", produces = MediaType.APPLICATION_ATOM_XML_VALUE)
    public String atom() {
        return feedService.buildAtomFeed();
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String sitemap() {
        return feedService.buildSitemap();
    }
}
