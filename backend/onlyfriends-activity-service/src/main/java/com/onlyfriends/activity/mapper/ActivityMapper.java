package com.onlyfriends.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.onlyfriends.activity.entity.Activity;
import org.apache.ibatis.annotations.Update;

public interface ActivityMapper extends BaseMapper<Activity> {
    @Update("""
            UPDATE activity
            SET current_count = current_count + 1
            WHERE id = #{activityId}
              AND deleted = 0
              AND (max_participants = 0 OR current_count < max_participants)
            """)
    int tryOccupySeat(Long activityId);

    @Update("""
            UPDATE activity
            SET current_count = CASE WHEN current_count > 0 THEN current_count - 1 ELSE 0 END
            WHERE id = #{activityId}
              AND deleted = 0
            """)
    int releaseSeat(Long activityId);
}
