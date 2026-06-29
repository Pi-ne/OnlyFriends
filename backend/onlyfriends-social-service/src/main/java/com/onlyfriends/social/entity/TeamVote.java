package com.onlyfriends.social.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("team_vote")
public class TeamVote {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long teamId;
    private Long creatorId;
    private String title;
    private String description;
    private Integer multiple;
    private Integer status;
    private LocalDateTime deadline;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
