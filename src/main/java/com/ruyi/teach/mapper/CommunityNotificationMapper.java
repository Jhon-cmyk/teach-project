package com.ruyi.teach.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruyi.teach.model.entity.CommunityNotification;
import com.ruyi.teach.model.vo.CommunityNotificationVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CommunityNotificationMapper extends BaseMapper<CommunityNotification> {

    IPage<CommunityNotificationVO> selectNotificationPage(Page<CommunityNotificationVO> page,
                                                          @Param("userId") Long userId,
                                                          @Param("isRead") Integer isRead,
                                                          @Param("type") String type);

    @Select("SELECT COUNT(*) FROM community_notification " +
            "WHERE user_id = #{userId} AND is_read = 0 AND is_delete = 0")
    int countUnreadByUserId(@Param("userId") Long userId);

    @Update("UPDATE community_notification " +
            "SET is_read = 1, update_time = NOW() " +
            "WHERE id = #{id} AND user_id = #{userId} AND is_delete = 0")
    int markAsRead(@Param("id") Long id, @Param("userId") Long userId);

    @Update("UPDATE community_notification " +
            "SET is_read = 1, update_time = NOW() " +
            "WHERE user_id = #{userId} AND is_read = 0 AND is_delete = 0")
    int markAllAsRead(@Param("userId") Long userId);
}