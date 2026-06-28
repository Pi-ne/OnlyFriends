package com.ququ.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ququ.activity.entity.ActivityTag;
import org.apache.ibatis.annotations.Update;

public interface ActivityTagMapper extends BaseMapper<ActivityTag> {
    @Update("UPDATE activity_tag SET usage_count = usage_count + 1 WHERE name = #{name}")
    int incrementUsage(String name);
}
