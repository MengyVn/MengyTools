package com.mengy.tools.mapper;

import com.mengy.tools.dto.CommunityTagDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 社区标签 Mapper（只读）。
 * 标签数据在 blog_tag / blog_article_tag，此前后端从未使用（无对应 Mapper），
 * 社区化后首次接入。
 */
@Mapper
public interface CommunityTagMapper {

    /** 标签列表（含该标签下已发布文章数，按文章数倒序）。 */
    @Select("""
        SELECT t.id, t.name, t.slug,
               (SELECT COUNT(*) FROM blog_article_tag at
                  JOIN blog_article a ON a.id = at.article_id AND a.deleted = 0 AND a.status = 1
                 WHERE at.tag_id = t.id) AS article_count
        FROM blog_tag t
        WHERE t.deleted = 0
        ORDER BY article_count DESC, t.id ASC
        """)
    List<CommunityTagDTO> selectTagList();

    /** 仅取有文章的热门标签（侧栏用）。 */
    @Select("""
        SELECT t.id, t.name, t.slug,
               (SELECT COUNT(*) FROM blog_article_tag at
                  JOIN blog_article a ON a.id = at.article_id AND a.deleted = 0 AND a.status = 1
                 WHERE at.tag_id = t.id) AS article_count
        FROM blog_tag t
        WHERE t.deleted = 0
        HAVING article_count > 0
        ORDER BY article_count DESC, t.id ASC
        LIMIT #{limit}
        """)
    List<CommunityTagDTO> selectHotTags(@Param("limit") int limit);

    /** 按 slug 解析标签ID（前端用 slug 做 SEO 友好 URL）。 */
    @Select("SELECT id FROM blog_tag WHERE slug = #{slug} AND deleted = 0")
    Long selectIdBySlug(@Param("slug") String slug);

    /** 标签总数（侧栏统计）。 */
    @Select("SELECT COUNT(*) FROM blog_tag WHERE deleted = 0")
    Long countTags();
}
