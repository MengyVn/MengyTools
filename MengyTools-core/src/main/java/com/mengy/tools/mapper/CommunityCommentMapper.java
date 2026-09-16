package com.mengy.tools.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mengy.tools.dto.CommunityCommentAdminDTO;
import com.mengy.tools.dto.CommunityCommentDTO;
import com.mengy.tools.dto.CommunityUserCommentDTO;
import com.mengy.tools.entity.CommunityComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 社区评论 Mapper：前台只读查询 + 后台审核列表。
 */
@Mapper
public interface CommunityCommentMapper extends BaseMapper<CommunityComment> {

    /** 顶层评论列（多处复用，避免重复书写字段清单） */
    String TOP_COLUMNS = """
        id, article_id, root_id, parent_id, floor, reply_to_user_id, reply_to_name,
        author_id, author_name, author_avatar, content, content_html, like_count, create_time
        """;

    /**
     * 顶层评论分页（parent_id=0 且已发布）。
     * sort: asc（默认，按楼层正序） / desc（按楼层倒序，看最新讨论）
     */
    @Select("""
        <script>
        SELECT id, article_id, root_id, parent_id, floor, reply_to_user_id, reply_to_name,
               author_id, author_name, author_avatar, content, content_html, like_count, create_time
        FROM community_comment
        WHERE deleted = 0 AND status = 1 AND article_id = #{articleId} AND parent_id = 0
        <choose>
          <when test="sort != null and sort == 'desc'"> ORDER BY floor DESC, id DESC </when>
          <otherwise> ORDER BY floor ASC, id ASC </otherwise>
        </choose>
        </script>
        """)
    IPage<CommunityCommentDTO> selectTopPage(IPage<CommunityCommentDTO> page,
                                             @Param("articleId") Long articleId,
                                             @Param("sort") String sort);

    /**
     * 批量取某批顶层评论下的全部子回复（调用方保证 rootIds 非空）。
     */
    @Select("""
        <script>
        SELECT id, article_id, root_id, parent_id, floor, reply_to_user_id, reply_to_name,
               author_id, author_name, author_avatar, content, content_html, like_count, create_time
        FROM community_comment
        WHERE deleted = 0 AND status = 1 AND root_id IN
        <foreach collection="rootIds" item="rid" open="(" separator="," close=")">#{rid}</foreach>
        ORDER BY id ASC
        </script>
        """)
    List<CommunityCommentDTO> selectRepliesByRootIds(@Param("rootIds") List<Long> rootIds);

    /** 某用户的评论分页（带所属文章标题）。 */
    @Select("""
        SELECT c.id, c.article_id, a.title AS article_title, c.root_id, c.parent_id,
               c.content, c.content_html, c.like_count, c.create_time
        FROM community_comment c
        LEFT JOIN blog_article a ON a.id = c.article_id
        WHERE c.deleted = 0 AND c.status = 1 AND c.author_id = #{userId}
        ORDER BY c.id DESC
        """)
    IPage<CommunityUserCommentDTO> selectUserCommentPage(IPage<CommunityUserCommentDTO> page,
                                                        @Param("userId") Long userId);

    /** 按状态统计评论数（后台统计与待审徽标）。 */
    @Select("SELECT COUNT(*) FROM community_comment WHERE deleted = 0 AND status = #{status}")
    Long countByStatus(@Param("status") Integer status);

    /**
     * 后台评论审核列表（可按状态/文章/关键词过滤）。
     * 不过滤 status：待审、已发布、已屏蔽都要能查。
     */
    @Select("""
        <script>
        SELECT c.id, c.article_id, a.title AS article_title, c.root_id, c.parent_id, c.floor,
               c.author_id, c.author_name, c.content, c.status, c.like_count, c.ip, c.create_time
        FROM community_comment c
        LEFT JOIN blog_article a ON a.id = c.article_id
        WHERE c.deleted = 0
        <if test="status != null"> AND c.status = #{status} </if>
        <if test="articleId != null"> AND c.article_id = #{articleId} </if>
        <if test="keyword != null and keyword != ''">
          AND (c.content LIKE CONCAT('%', #{keyword}, '%')
               OR c.author_name LIKE CONCAT('%', #{keyword}, '%')
               OR a.title LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        ORDER BY c.status ASC, c.id DESC
        </script>
        """)
    IPage<CommunityCommentAdminDTO> selectAdminPage(IPage<CommunityCommentAdminDTO> page,
                                                    @Param("status") Integer status,
                                                    @Param("articleId") Long articleId,
                                                    @Param("keyword") String keyword);

    /** 当前最大楼层号（顶层评论），用于分配新楼层 */
    @Select("SELECT IFNULL(MAX(floor), 0) FROM community_comment WHERE article_id = #{articleId} AND parent_id = 0")
    Integer selectMaxFloor(@Param("articleId") Long articleId);

    /** 顶层评论的可见子回复数（删除/屏蔽后回退计数用） */
    @Select("SELECT COUNT(*) FROM community_comment WHERE root_id = #{rootId} AND deleted = 0 AND status = 1")
    int countVisibleReplies(@Param("rootId") Long rootId);

    /** 重算并回填评论点赞数（与文章同理，重算取事实表，避免计数漂移） */
    @Update("""
        UPDATE community_comment c
           SET c.like_count = (SELECT COUNT(*) FROM community_reaction r
                                WHERE r.target_type = 'comment' AND r.target_id = c.id AND r.type = 'like')
         WHERE c.id = #{id}
        """)
    int recountLikes(@Param("id") Long id);

    /** 重算后回读评论点赞数 */
    @Select("SELECT like_count FROM community_comment WHERE id = #{id}")
    Integer selectLikeCount(@Param("id") Long id);

    /** 评论状态流转（审核用）：0待审 1已发布 2已屏蔽 */
    @Update("UPDATE community_comment SET status = #{status} WHERE id = #{id} AND deleted = 0")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /** 按 id 取简要信息（校验归属与归属文章用） */
    @Select("""
        SELECT id, article_id, root_id, parent_id, floor, author_id, author_name, status
        FROM community_comment WHERE id = #{id} AND deleted = 0
        """)
    CommunityComment selectBrief(@Param("id") Long id);

    /** 连带软删除某顶层评论下的所有子回复 */
    @Update("UPDATE community_comment SET deleted = 1 WHERE root_id = #{rootId} AND deleted = 0")
    int softDeleteReplies(@Param("rootId") Long rootId);

    /** 某文章的已发布顶层评论数（与 blog_article.comment_count 口径一致） */
    @Select("""
        SELECT COUNT(*) FROM community_comment
        WHERE article_id = #{articleId} AND parent_id = 0 AND deleted = 0 AND status = 1
        """)
    int countVisibleTopLevel(@Param("articleId") Long articleId);
}
