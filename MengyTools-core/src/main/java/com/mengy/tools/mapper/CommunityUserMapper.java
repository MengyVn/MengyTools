package com.mengy.tools.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mengy.tools.dto.CommunityUserAdminDTO;
import com.mengy.tools.dto.CommunityUserBriefDTO;
import com.mengy.tools.dto.CommunityUserProfileDTO;
import com.mengy.tools.dto.CommunityUserStateDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 社区用户公开信息 Mapper（只读）。
 *
 * 同样不改 SysUser 实体：社区需要的 signature 列属于本次迁移新增，
 * 走独立 mapper 可避免门户/管理端查询在迁移前带上不存在的列。
 */
@Mapper
public interface CommunityUserMapper {

    /**
     * 公开用户主页（含统计：评论数、文章数、累计获赞、关注数、粉丝数）。
     * 不返回 username，避免账户枚举。
     * 关注/粉丝计数过滤掉已注销与被封禁的账号，口径与关注/粉丝列表一致。
     */
    @Select("""
        SELECT u.id, u.nickname, u.avatar, u.signature, u.create_time,
               (SELECT COUNT(*) FROM community_comment c
                 WHERE c.author_id = u.id AND c.deleted = 0 AND c.status = 1) AS comment_count,
               (SELECT COUNT(*) FROM blog_article a
                 WHERE a.author_id = u.id AND a.deleted = 0 AND a.status = 1) AS article_count,
               (SELECT IFNULL(SUM(c.like_count), 0) FROM community_comment c
                 WHERE c.author_id = u.id AND c.deleted = 0 AND c.status = 1) AS received_like_count,
               (SELECT COUNT(*) FROM community_follow f
                  JOIN sys_user tu ON tu.id = f.target_id AND tu.deleted = 0 AND tu.status = 1
                 WHERE f.user_id = u.id AND f.target_type = 'user') AS following_count,
               (SELECT COUNT(*) FROM community_follow f
                  JOIN sys_user fu ON fu.id = f.user_id AND fu.deleted = 0 AND fu.status = 1
                 WHERE f.target_type = 'user' AND f.target_id = u.id) AS follower_count
        FROM sys_user u
        WHERE u.id = #{id} AND u.deleted = 0 AND u.status = 1
        """)
    CommunityUserProfileDTO selectPublicProfile(@Param("id") Long id);

    /** 触发者简要资料（昵称/头像/签名），写通知时冗余进通知行 */
    @Select("SELECT id, nickname, avatar, signature FROM sys_user WHERE id = #{id} AND deleted = 0")
    CommunityUserBriefDTO selectBriefProfile(@Param("id") Long id);

    /** 活跃用户（按已发布评论数倒序）。 */
    @Select("""
        SELECT u.id, u.nickname, u.avatar, COUNT(c.id) AS comment_count
        FROM sys_user u
        JOIN community_comment c ON c.author_id = u.id AND c.deleted = 0 AND c.status = 1
        WHERE u.deleted = 0 AND u.status = 1
        GROUP BY u.id, u.nickname, u.avatar
        ORDER BY comment_count DESC, u.id ASC
        LIMIT #{limit}
        """)
    List<CommunityUserBriefDTO> selectActiveUsers(@Param("limit") int limit);

    /** 注册用户总数（侧栏统计）。 */
    @Select("SELECT COUNT(*) FROM sys_user WHERE deleted = 0 AND status = 1")
    Long countUsers();

    /**
     * 后台「用户治理」分页列表。
     *
     * @param keyword       匹配登录名/昵称
     * @param onlyCommented true=只看发过评论的用户（社区治理默认视图）
     */
    @Select("""
        <script>
        SELECT u.id, u.username, u.nickname, u.avatar, u.signature, u.status, u.mute_until, u.create_time,
               (SELECT COUNT(*) FROM community_comment c
                 WHERE c.author_id = u.id AND c.deleted = 0 AND c.status = 1) AS comment_count
        FROM sys_user u
        WHERE u.deleted = 0
        <if test="keyword != null and keyword != ''">
          AND (u.username LIKE CONCAT('%', #{keyword}, '%') OR u.nickname LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        <if test="onlyCommented">
          AND EXISTS (SELECT 1 FROM community_comment c2 WHERE c2.author_id = u.id AND c2.deleted = 0)
        </if>
        ORDER BY comment_count DESC, u.id ASC
        </script>
        """)
    IPage<CommunityUserAdminDTO> selectAdminUserPage(IPage<CommunityUserAdminDTO> page,
                                                     @Param("keyword") String keyword,
                                                     @Param("onlyCommented") boolean onlyCommented);

    /**
     * 社区写入侧的用户状态（账号状态 / 禁言到期 / 注册时间）。
     * 独立查询，不改动 SysUser 实体，避免影响门户与管理端既有查询。
     */
    @Select("SELECT id, status, mute_until, create_time FROM sys_user WHERE id = #{id} AND deleted = 0")
    CommunityUserStateDTO selectUserState(@Param("id") Long id);

    /** 按昵称找用户（用于 @提及 生成通知）。 */
    @Select("SELECT id FROM sys_user WHERE nickname = #{nickname} AND deleted = 0 AND status = 1 LIMIT 1")
    Long selectIdByNickname(@Param("nickname") String nickname);

    /** 更新禁言到期时间（null=解除禁言）。 */
    @Update("UPDATE sys_user SET mute_until = #{muteUntil} WHERE id = #{id} AND deleted = 0")
    int updateMuteUntil(@Param("id") Long id, @Param("muteUntil") LocalDateTime muteUntil);

    /** 更新个人签名（社区资料） */
    @Update("UPDATE sys_user SET signature = #{signature} WHERE id = #{id} AND deleted = 0")
    int updateSignature(@Param("id") Long id, @Param("signature") String signature);

    /** 封禁/解封（status: 0封禁 1正常）。 */
    @Update("UPDATE sys_user SET status = #{status} WHERE id = #{id} AND deleted = 0")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}
