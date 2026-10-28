package cn.whaa.blog.infrastructure.persistence.mapper;

import cn.whaa.blog.infrastructure.persistence.entity.PostEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface PostMapper extends BaseMapper<PostEntity> {

    @Select("""
            SELECT * FROM post
            WHERE status = 'PUBLISHED'
              AND (published_at IS NULL OR published_at <= NOW())
              AND MATCH(title, description, excerpt, content_md) AGAINST(#{keyword} IN NATURAL LANGUAGE MODE)
            ORDER BY published_at DESC
            LIMIT #{limit}
            """)
    List<PostEntity> searchPublished(@Param("keyword") String keyword, @Param("limit") int limit);

    @Update("UPDATE post SET view_count = view_count + 1 WHERE id = #{id}")
    int incrementViewCount(@Param("id") Long id);
}
