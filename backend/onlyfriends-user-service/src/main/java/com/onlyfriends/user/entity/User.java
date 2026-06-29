package com.onlyfriends.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("`user`")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String email;
    private String passwordHash;
    private String nickname;
    private String avatarUrl;
    private Integer gender;
    private LocalDate birthday;
    private String bio;
    private String interestTags;
    private Integer userType;
    private Integer status;
    private Integer creditScore;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String activateToken;
    private LocalDateTime banExpireAt;
    @TableLogic
    private Integer deleted;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
