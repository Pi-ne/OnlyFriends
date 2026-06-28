package com.ququ.social.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("team_vote_record")
public class TeamVoteRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long voteId;
    private Long optionId;
    private Long userId;
    private LocalDateTime createdAt;
}
