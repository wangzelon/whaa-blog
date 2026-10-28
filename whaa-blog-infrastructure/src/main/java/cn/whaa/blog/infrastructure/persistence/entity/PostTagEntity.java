package cn.whaa.blog.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("post_tag")
public class PostTagEntity {
    private Long postId;
    private Long tagId;
}
