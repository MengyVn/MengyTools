package com.mengy.tools.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mengy.tools.dto.CommunityArticleDTO;
import com.mengy.tools.dto.CommunityArticleDetailDTO;
import com.mengy.tools.dto.CommunityCountDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 社区文章查询 Mapper（只读）。
 *
 * 为什么单独建 Mapper 而不动 BlogArticle 实体：门户接口处于冻结状态，
 * 若给 BlogArticle 加 comment_count/like_count 等新列字段，门户查询会带上这些列，
 * 在“代码已更新但迁移脚本未执行”的窗口期内会直接报 Unknown column。
 * 这里用独立 mapper 显式 select，把社区功能与门户彻底解耦。
 *
 * 注意：本 Mapper 的 SQL 依赖 mengy_tools_community.sql 增加的互动列。
 */
@Mapper
public interface CommunityArticleMapper {

    /**
     * 社区文章分页。
     * sort: latest（默认，按发布时间倒序） / hot（按评论数、浏览数倒序）
     */
    @Select("""
        <script>
        SELECT a.id, a.title, a.summary, a.cover, a.category_id, c.name AS category_name,
               a.author_id, u.nickname AS author_name, a.is_top, a.view_count,
               a.comment_count, a.like_count, a.favorite_count, a.publish_time, a.create_time
        FROM blog_article a
        LEFT JOIN blog_category c ON c.id = a.category_id AND c.deleted = 0
        LEFT JOIN sys_user u ON u.id = a.author_id AND u.deleted = 0
        WHERE a.deleted = 0 AND a.status = 1
        <if test="categoryId != null"> AND a.category_id = #{categoryId} </if>
        <if test="tagId != null">
          AND EXISTS (SELECT 1 FROM blog_article_tag t WHERE t.article_id = a.id AND t.tag_id = #{tagId})
        </if>
        <if test="keyword != null and keyword != ''">
          AND (a.title LIKE CONCAT('%', #{keyword}, '%') OR a.summary LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        ORDER BY a.is_top DESC,
        <choose>
          <when test="sort != null and sort == 'hot'">
            (a.comment_count * 3 + a.like_count * 2 + a.favorite_count * 2) DESC, a.view_count DESC, a.id DESC
          </when>
          <otherwise> a.publish_time DESC, a.id DESC </otherwise>
        </choose>
        </script>
        """)
    IPage<CommunityArticleDTO> selectCommunityPage(IPage<CommunityArticleDTO> page,
                                                   @Param("categoryId") Long categoryId,
                                                   @Param("tagId") Long tagId,
                                                   @Param("keyword") String keyword,
                                                   @Param("sort") String sort);

    /**
     * 全文检索（ngram 全文索引，相关度排序）。
     *
     * 依赖 mengy_tools_community_search.sql 创建的 ft_article_search 索引；
     * 若索引不存在，调用方会捕获异常并降级为 LIKE 检索（见 CommunityPortalController#search）。
     */
    @Select("""
        <script>
        SELECT a.id, a.title, a.summary, a.cover, a.category_id, c.name AS category_name,
               a.author_id, u.nickname AS author_name, a.is_top, a.view_count,
               a.comment_count, a.like_count, a.favorite_count, a.publish_time, a.create_time
        FROM blog_article a
        LEFT JOIN blog_category c ON c.id = a.category_id AND c.deleted = 0
        LEFT JOIN sys_user u ON u.id = a.author_id AND u.deleted = 0
        WHERE a.deleted = 0 AND a.status = 1
          AND MATCH(a.title, a.summary, a.content) AGAINST(#{keyword} IN NATURAL LANGUAGE MODE)
        <if test="categoryId != null"> AND a.category_id = #{categoryId} </if>
        <if test="tagId != null">
          AND EXISTS (SELECT 1 FROM blog_article_tag t WHERE t.article_id = a.id AND t.tag_id = #{tagId})
        </if>
        ORDER BY MATCH(a.title, a.summary, a.content) AGAINST(#{keyword} IN NATURAL LANGUAGE MODE) DESC,
                 a.publish_time DESC, a.id DESC
        </script>
        """)
    IPage<CommunityArticleDTO> selectSearchPage(IPage<CommunityArticleDTO> page,
                                                @Param("keyword") String keyword,
                                                @Param("categoryId") Long categoryId,
                                                @Param("tagId") Long tagId);

    /**
     * 社区文章详情（仅已发布）。
     */
    @Select("""
        SELECT a.id, a.title, a.summary, a.cover, a.category_id, c.name AS category_name,
               a.author_id, u.nickname AS author_name, a.content, a.content_html, a.content_format,
               a.allow_comment, a.is_top, a.view_count, a.comment_count, a.like_count,
               a.favorite_count, a.publish_time, a.create_time, a.update_time
        FROM blog_article a
        LEFT JOIN blog_category c ON c.id = a.category_id AND c.deleted = 0
        LEFT JOIN sys_user u ON u.id = a.author_id AND u.deleted = 0
        WHERE a.id = #{id} AND a.deleted = 0 AND a.status = 1
        """)
    CommunityArticleDetailDTO selectCommunityDetail(@Param("id") Long id);

    /** 已发布文章总数（侧栏统计）。 */
    @Select("SELECT COUNT(*) FROM blog_article WHERE deleted = 0 AND status = 1")
    Long countPublished();

    /**
     * 排行榜用：按评论数倒序取前 N 篇已发布文章。
     */
    @Select("""
        SELECT a.id, a.title, a.summary, a.cover, a.category_id, c.name AS category_name,
               a.author_id, u.nickname AS author_name, a.is_top, a.view_count,
               a.comment_count, a.like_count, a.favorite_count, a.publish_time, a.create_time
        FROM blog_article a
        LEFT JOIN blog_category c ON c.id = a.category_id AND c.deleted = 0
        LEFT JOIN sys_user u ON u.id = a.author_id AND u.deleted = 0
        WHERE a.deleted = 0 AND a.status = 1
        ORDER BY a.comment_count DESC, a.view_count DESC, a.id DESC
        LIMIT #{limit}
        """)
    java.util.List<CommunityArticleDTO> selectHotArticles(@Param("limit") int limit);

    /** 校验文章可评论：存在、未删、已发布。返回 id 或 null。 */
    @Select("SELECT id FROM blog_article WHERE id = #{id} AND deleted = 0 AND status = 1")
    Long selectPublishedId(@Param("id") Long id);

    /** 是否允许评论（1允许 0关闭） */
    @Select("SELECT allow_comment FROM blog_article WHERE id = #{id} AND deleted = 0")
    Integer selectAllowComment(@Param("id") Long id);

    /**
     * 重算并回填文章的互动计数（评论数/点赞数/收藏数）。
     *
     * 采用「重算」而不是「增减」：评论被屏蔽/删除、回复连带删除、点赞取消等路径很多，
     * 增减很容易漂移；重算每次都以事实表为准，天然自愈，代价是一次带子查询的 UPDATE。
     * 注意显式写回 update_time：回填计数不应被误认为「文章刚被编辑」。
     */
    @Update("""
        UPDATE blog_article a
           SET a.comment_count = (SELECT COUNT(*) FROM community_comment c
                                   WHERE c.article_id = a.id AND c.parent_id = 0
                                     AND c.deleted = 0 AND c.status = 1),
               a.like_count = (SELECT COUNT(*) FROM community_reaction r
                                WHERE r.target_type = 'article' AND r.target_id = a.id AND r.type = 'like'),
               a.favorite_count = (SELECT COUNT(*) FROM community_reaction r
                                    WHERE r.target_type = 'article' AND r.target_id = a.id AND r.type = 'favorite'),
               a.update_time = a.update_time
         WHERE a.id = #{id} AND a.deleted = 0
        """)
    int recountArticleStats(@Param("id") Long id);

    /** 重算后回读最新计数（返回给前端更新按钮与数字） */
    @Select("SELECT like_count, favorite_count, comment_count FROM blog_article WHERE id = #{id}")
    CommunityCountDTO selectCounts(@Param("id") Long id);
}
