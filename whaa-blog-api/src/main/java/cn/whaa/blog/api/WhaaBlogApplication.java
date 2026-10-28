package cn.whaa.blog.api;

import cn.whaa.blog.infrastructure.config.JwtProperties;
import cn.whaa.blog.infrastructure.config.MinioProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@ComponentScan(basePackages = "cn.whaa.blog")
@EnableConfigurationProperties({JwtProperties.class, MinioProperties.class})
@EnableAsync
public class WhaaBlogApplication {

    public static void main(String[] args) {
        SpringApplication.run(WhaaBlogApplication.class, args);
    }
}
