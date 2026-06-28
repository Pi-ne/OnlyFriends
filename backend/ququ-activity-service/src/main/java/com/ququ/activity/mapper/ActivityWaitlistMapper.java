package com.ququ.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ququ.activity.entity.ActivityWaitlist;
import org.apache.ibatis.annotations.Select;

public interface ActivityWaitlistMapper extends BaseMapper<ActivityWaitlist> {
    @Select("SELECT COALESCE(MAX(queue_no), 0) + 1 FROM activity_waitlist WHERE activity_id = #{activityId}")
    Integer nextQueueNo(Long activityId);
}
