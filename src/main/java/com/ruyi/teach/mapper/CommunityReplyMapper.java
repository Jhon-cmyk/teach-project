package com.ruyi.teach.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruyi.teach.model.entity.CommunityReply;
import com.ruyi.teach.model.vo.MyCommunityReplyVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CommunityReplyMapper extends BaseMapper<CommunityReply> {

    /**
     * 统计“我参与回复过的讨论”总数（按 post 去重）
     */
    @Select("SELECT COUNT(DISTINCT post_id) " +
            "FROM community_reply " +
            "WHERE user_id = #{userId} AND is_delete = 0")
    long countMyRepliedPosts(@Param("userId") Long userId);

    /**
     * 分页查询“我参与回复过的讨论”
     */
    @Select("SELECT " +
            "  p.id AS discussionId, " +
            "  p.title AS title, " +
            "  p.course_id AS courseId, " +
            "  p.course_name AS courseName, " +
            "  DATE_FORMAT(t.my_last_reply_time, '%Y-%m-%d %H:%i') AS myLastReplyTime, " +
            "  COALESCE(p.reply_count, 0) AS replyCount, " +
            "  CASE " +
            "    WHEN TIMESTAMPDIFF(MINUTE, p.last_active_time, NOW()) < 1 THEN '刚刚' " +
            "    WHEN TIMESTAMPDIFF(MINUTE, p.last_active_time, NOW()) < 60 THEN CONCAT(TIMESTAMPDIFF(MINUTE, p.last_active_time, NOW()), ' 分钟前') " +
            "    WHEN TIMESTAMPDIFF(HOUR, p.last_active_time, NOW()) < 24 THEN CONCAT(TIMESTAMPDIFF(HOUR, p.last_active_time, NOW()), ' 小时前') " +
            "    WHEN TIMESTAMPDIFF(DAY, p.last_active_time, NOW()) = 1 THEN '昨天' " +
            "    WHEN TIMESTAMPDIFF(DAY, p.last_active_time, NOW()) < 30 THEN CONCAT(TIMESTAMPDIFF(DAY, p.last_active_time, NOW()), ' 天前') " +
            "    ELSE DATE_FORMAT(p.last_active_time, '%m-%d') " +
            "  END AS lastActiveTime, " +
            "  UNIX_TIMESTAMP(p.last_active_time) * 1000 AS lastActiveTimestamp " +
            "FROM community_post p " +
            "INNER JOIN ( " +
            "  SELECT post_id, MAX(create_time) AS my_last_reply_time " +
            "  FROM community_reply " +
            "  WHERE user_id = #{userId} AND is_delete = 0 " +
            "  GROUP BY post_id " +
            ") t ON t.post_id = p.id " +
            "WHERE p.is_delete = 0 " +
            "ORDER BY t.my_last_reply_time DESC " +
            "LIMIT #{offset}, #{pageSize}")
    List<MyCommunityReplyVO> selectMyRepliedPosts(@Param("userId") Long userId,
                                                  @Param("offset") long offset,
                                                  @Param("pageSize") long pageSize);

    /**
     * 查询参与过某条讨论回复的用户ID（用于“讨论有新动态”提醒）
     */
    @Select("SELECT DISTINCT user_id " +
            "FROM community_reply " +
            "WHERE post_id = #{postId} " +
            "  AND is_delete = 0 " +
            "  AND user_id IS NOT NULL")
    List<Long> selectParticipantUserIds(@Param("postId") Long postId);
}