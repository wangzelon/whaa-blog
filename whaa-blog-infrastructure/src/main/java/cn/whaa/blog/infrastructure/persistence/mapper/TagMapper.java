package cn.whaa.blog.infrastructure.persistence.mapper;

import cn.whaa.blog.infrastructure.persistence.entity.TagEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface TagMapper extends BaseMapper<TagEntity> {

    @Select("""
            SELECT t.id, t.name, t.slug, COUNT(pt.post_id) AS weight
            FROM tag t
            LEFT JOIN post_tag pt ON t.id = pt.tag_id
            LEFT JOIN post p ON pt.post_id = p.id AND p.status = 'PUBLISHED'
            GROUP BY t.id, t.name, t.slug
            ORDER BY weight DESC, t.name
            """)
    List<Map<String, Object>> selectTagCloud();
}
