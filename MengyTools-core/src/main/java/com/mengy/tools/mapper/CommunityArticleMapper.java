package com.mengy.tools.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mengy.tools.dto.CommunityArticleDTO;
import com.mengy.tools.dto.CommunityBoardDTO;
import com.mengy.tools.dto.CommunityArticleDetailDTO;
import com.mengy.tools.dto.CommunityCountDTO;
import com.mengy.tools.dto.CommunityPostDTO;
import com.mengy.tools.dto.CommunityPostInsert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

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
        ORDER BY MATCH(a.title, a.summary, a.content) AGAINST(#{keyword} IN NATURAL LANGUAGE MODE) DESC,
                 a.publish_time DESC, a.id DESC
        </script>
        """)
    IPage<CommunityArticleDTO> selectSearchPage(IPage<CommunityArticleDTO> page,
                                                @Param("keyword") String keyword,
                                                @Param("categoryId") Long categoryId);

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
    List<CommunityArticleDTO> selectHotArticles(@Param("limit") int limit);

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

    // ==================== 用户发帖（content_type='community'） ====================

    /**
     * 插入社区帖子。content_type 固定为 community，publish_time 取当前时间；
     * 主键回填到入参 bean 的 id 上。
     */
    @Insert("""
        INSERT INTO blog_article
            (content_type, title, summary, content, content_html, content_format, cover,
             category_id, author_id, status, allow_comment, is_top,
             view_count, comment_count, like_count, favorite_count, publish_time, deleted)
        VALUES
            ('community', #{title}, #{summary}, #{content}, #{contentHtml}, 'markdown', #{cover},
             #{categoryId}, #{authorId}, #{status}, #{allowComment}, 0,
             0, 0, 0, 0, NOW(), 0)
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertPost(CommunityPostInsert post);

    /** 更新自己的帖子（同时校验归属与类型，返回 0 表示无权或不存在） */
    @Update("""
        UPDATE blog_article
           SET title = #{title}, summary = #{summary}, content = #{content}, content_html = #{contentHtml},
               cover = #{cover}, category_id = #{categoryId}, status = #{status},
               allow_comment = #{allowComment}, update_time = NOW()
         WHERE id = #{id} AND author_id = #{authorId}
           AND content_type = 'community' AND deleted = 0
        """)
    int updateOwnPost(CommunityPostInsert post);

    /** 软删除自己的帖子 */
    @Update("""
        UPDATE blog_article SET deleted = 1, update_time = NOW()
         WHERE id = #{id} AND author_id = #{authorId}
           AND content_type = 'community' AND deleted = 0
        """)
    int softDeleteOwnPost(@Param("id") Long id, @Param("authorId") Long authorId);

    /** 后台软删除帖子 */
    @Update("""
        UPDATE blog_article SET deleted = 1, update_time = NOW()
         WHERE id = #{id} AND content_type = 'community' AND deleted = 0
        """)
    int softDeletePostByAdmin(@Param("id") Long id);

    /** 后台审核：修改帖子状态 */
    @Update("""
        UPDATE blog_article SET status = #{status}, update_time = NOW()
         WHERE id = #{id} AND content_type = 'community' AND deleted = 0
        """)
    int updatePostStatus(@Param("id") Long id, @Param("status") Integer status);

    /** 帖子归属与状态（编辑/删除前的权限校验） */
    @Select("""
        SELECT id, author_id AS authorId, status, title
          FROM blog_article
         WHERE id = #{id} AND content_type = 'community' AND deleted = 0
        """)
    CommunityPostDTO selectOwnPostBrief(@Param("id") Long id);

    /** 作者视角的帖子详情（含待审/已屏蔽，用于编辑页回填 Markdown 原文） */
    @Select("""
        SELECT a.id, a.title, a.summary, a.cover, a.category_id, c.name AS category_name,
               a.author_id, u.nickname AS author_name, a.content, a.content_html, a.content_format,
               a.allow_comment, a.is_top, a.view_count, a.comment_count, a.like_count,
               a.favorite_count, a.publish_time, a.create_time, a.update_time
          FROM blog_article a
          LEFT JOIN blog_category c ON c.id = a.category_id AND c.deleted = 0
          LEFT JOIN sys_user u ON u.id = a.author_id AND u.deleted = 0
         WHERE a.id = #{id} AND a.author_id = #{authorId}
           AND a.content_type = 'community' AND a.deleted = 0
        """)
    CommunityArticleDetailDTO selectOwnPostDetail(@Param("id") Long id, @Param("authorId") Long authorId);

    /** 我的帖子分页（含待审、已屏蔽，按状态过滤可选） */
    @Select("""
        <script>
        SELECT id, title, summary, cover, status, view_count, comment_count, like_count,
               publish_time, create_time, update_time
          FROM blog_article
         WHERE deleted = 0 AND content_type = 'community' AND author_id = #{userId}
        <if test="status != null"> AND status = #{status} </if>
         ORDER BY id DESC
        </script>
        """)
    IPage<CommunityPostDTO> selectMyPosts(IPage<CommunityPostDTO> page,
                                         @Param("userId") Long userId,
                                         @Param("status") Integer status);

    /** 后台帖子列表（可按状态/关键词过滤） */
    @Select("""
        <script>
        SELECT a.id, a.title, a.summary, a.cover, a.status, a.view_count, a.comment_count,
               a.like_count, a.publish_time, a.create_time, a.update_time,
               a.author_id, u.nickname AS author_name
          FROM blog_article a
          LEFT JOIN sys_user u ON u.id = a.author_id AND u.deleted = 0
         WHERE a.deleted = 0 AND a.content_type = 'community'
        <if test="status != null"> AND a.status = #{status} </if>
        <if test="keyword != null and keyword != ''">
          AND (a.title LIKE CONCAT('%', #{keyword}, '%')
               OR u.nickname LIKE CONCAT('%', #{keyword}, '%'))
        </if>
         ORDER BY a.status ASC, a.id DESC
        </script>
        """)
    IPage<CommunityPostDTO> selectAdminPostPage(IPage<CommunityPostDTO> page,
                                               @Param("status") Integer status,
                                               @Param("keyword") String keyword);

    /** 待审帖子数（后台徽标） */
    @Select("SELECT COUNT(*) FROM blog_article WHERE deleted = 0 AND content_type = 'community' AND status = 0")
    Long countPendingPosts();

    /** 帖子总数（后台徽标/统计） */
    @Select("SELECT COUNT(*) FROM blog_article WHERE deleted = 0 AND content_type = 'community'")
    Long countCommunityPosts();

    /**
     * 板块列表（分类即板块）：含已发布数量与最近一篇。
     * 用相关子查询而不是 JOIN，避免为一篇「最近文章」做额外去重。
     */
    @Select("""
        SELECT c.id, c.name, c.slug,
               (SELECT COUNT(*) FROM blog_article a
                 WHERE a.category_id = c.id AND a.deleted = 0 AND a.status = 1) AS post_count,
               (SELECT a.id FROM blog_article a
                 WHERE a.category_id = c.id AND a.deleted = 0 AND a.status = 1
                 ORDER BY a.publish_time DESC, a.id DESC LIMIT 1) AS latest_post_id,
               (SELECT a.title FROM blog_article a
                 WHERE a.category_id = c.id AND a.deleted = 0 AND a.status = 1
                 ORDER BY a.publish_time DESC, a.id DESC LIMIT 1) AS latest_post_title,
               (SELECT a.publish_time FROM blog_article a
                 WHERE a.category_id = c.id AND a.deleted = 0 AND a.status = 1
                 ORDER BY a.publish_time DESC, a.id DESC LIMIT 1) AS latest_post_time
          FROM blog_category c
         WHERE c.deleted = 0
         ORDER BY c.sort ASC, c.id ASC
        """)
    List<CommunityBoardDTO> selectBoardList();
}
