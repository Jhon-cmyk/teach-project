package com.ruyi.teach.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruyi.teach.model.entity.CommunityPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Date;

@Mapper
public interface CommunityPostMapper extends BaseMapper<CommunityPost> {

    /**
     * 统计今日新增的作业互助提问数
     */
    @Select("SELECT COUNT(*) FROM community_post WHERE post_type = 'homework' AND is_delete = 0 AND DATE(create_time) = CURDATE()")
    int countTodayHomeworkQuestions();

    /**
     * 统计本周新增的答疑精选数
     */
    @Select("SELECT COUNT(*) FROM community_featured_answer WHERE is_delete = 0 AND YEARWEEK(create_time, 1) = YEARWEEK(CURDATE(), 1)")
    int countWeeklyFeaturedAnswers();

    /**
     * 浏览量 +1
     */
    @Select("UPDATE community_post SET view_count = view_count + 1 WHERE id = #{id} AND is_delete = 0")
    void incrementViewCount(@Param("id") Long id);

    /**
     * 回复成功后同步更新帖子统计
     */
    @Update("UPDATE community_post " +
            "SET reply_count = reply_count + 1, " +
            "    last_active_time = #{now}, " +
            "    update_time = #{now}, " +
            "    is_teacher_answered = CASE WHEN #{isTeacher} = 1 THEN 1 ELSE is_teacher_answered END " +
            "WHERE id = #{postId} AND is_delete = 0")
    int updatePostAfterReply(@Param("postId") Long postId,
                             @Param("isTeacher") Integer isTeacher,
                             @Param("now") Date now);

    /**
     * 删除回复后重新同步帖子回复数、教师已答状态与最后活跃时间。
     */
    @Update("UPDATE community_post p " +
            "SET p.reply_count = (SELECT COUNT(*) FROM community_reply r WHERE r.post_id = p.id AND r.is_delete = 0), " +
            "    p.is_teacher_answered = CASE WHEN EXISTS (SELECT 1 FROM community_reply tr WHERE tr.post_id = p.id AND tr.is_teacher = 1 AND tr.is_delete = 0) THEN 1 ELSE 0 END, " +
            "    p.last_active_time = COALESCE((SELECT MAX(ar.create_time) FROM community_reply ar WHERE ar.post_id = p.id AND ar.is_delete = 0), p.create_time), " +
            "    p.update_time = #{now} " +
            "WHERE p.id = #{postId} AND p.is_delete = 0")
    int refreshPostAfterReplyDeleted(@Param("postId") Long postId,
                                     @Param("now") Date now);

    /**
     * 标记作业互助问题已解决
     */
    @Update("UPDATE community_post " +
            "SET status = 'resolved', " +
            "    update_time = #{now} " +
            "WHERE id = #{id} AND post_type = 'homework' AND is_delete = 0")
    int resolveHomeworkPost(@Param("id") Long id,
                            @Param("now") Date now);
}
