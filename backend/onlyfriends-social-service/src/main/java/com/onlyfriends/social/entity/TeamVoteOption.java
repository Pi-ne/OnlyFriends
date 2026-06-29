package com.onlyfriends.social.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("team_vote_option")
public class TeamVoteOption {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long voteId;
    private String content;
    private Integer voteCount;
    private Integer sortOrder;
}
