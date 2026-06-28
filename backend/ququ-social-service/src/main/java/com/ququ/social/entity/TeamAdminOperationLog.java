package com.ququ.social.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("team_admin_operation_log")
public class TeamAdminOperationLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long teamId;
    private Long adminId;
    private String operationType;
    private String reason;
    private LocalDateTime createdAt;
}
