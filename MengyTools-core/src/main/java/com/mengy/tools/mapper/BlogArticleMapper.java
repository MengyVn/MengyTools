package com.mengy.tools.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mengy.tools.dto.ArticleListItemDTO;
import com.mengy.tools.entity.BlogArticle;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 博客文章 Mapper。
 */
public interface BlogArticleMapper extends BaseMapper<BlogArticle> {

    /**
     * 阅读量自增（原子操作，避免读改写竞态）。
     */
    @Update("UPDATE blog_article SET view_count = view_count + 1 WHERE id = #{id} AND deleted = 0")
    int incrementViewCount(@Param("id") Long id);

    /**
     * 分页查询回收站文章（deleted = 1，绕过 @TableLogic 逻辑删除过滤）。
     * 关联 blog_category 取分类名。
     */
    @Select("""
        SELECT a.id, a.title, a.summary, a.cover, a.category_id,
               c.name AS category_name, a.view_count, a.is_top,
               a.status, a.publish_time, a.create_time
        FROM blog_article a
        LEFT JOIN blog_category c ON c.id = a.category_id
        WHERE a.deleted = 1
        ORDER BY a.update_time DESC
        """)
    IPage<ArticleListItemDTO> selectTrashPage(IPage<ArticleListItemDTO> page,
                                               @Param("title") String title);

    /**
     * 物理删除单条文章。
     */
    @Update("DELETE FROM blog_article WHERE id = #{id}")
    int hardDelete(@Param("id") Long id);

    /**
     * 恢复单条文章（把 deleted 改回 0）。
     */
    @Update("UPDATE blog_article SET deleted = 0 WHERE id = #{id}")
    int restore(@Param("id") Long id);

    /**
     * 是否站主博客（content_type=blog）。
     * 社区用户帖是同一张表里的 content_type=community，门户详情据此拒绝访问。
     */
    @org.apache.ibatis.annotations.Select(
            "SELECT COUNT(*) FROM blog_article WHERE id = #{id} AND content_type = 'blog' AND deleted = 0")
    int countBlogArticle(@org.apache.ibatis.annotations.Param("id") Long id);
}
