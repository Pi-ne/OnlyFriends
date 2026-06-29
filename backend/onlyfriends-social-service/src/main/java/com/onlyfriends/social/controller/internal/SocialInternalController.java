package com.onlyfriends.social.controller.internal;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.onlyfriends.common.response.Result;
import com.onlyfriends.social.entity.FriendRelation;
import com.onlyfriends.social.entity.TeamMember;
import com.onlyfriends.social.mapper.FriendRelationMapper;
import com.onlyfriends.social.mapper.TeamMemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/social")
public class SocialInternalController {
    private static final int FRIEND_ACTIVE = 1;
    private static final int MEMBER_ACTIVE = 1;

    private final FriendRelationMapper friendRelationMapper;
    private final TeamMemberMapper teamMemberMapper;

    @GetMapping("/friends/check")
    public Result<Boolean> areFriends(@RequestParam Long userIdA, @RequestParam Long userIdB) {
        Long min = Math.min(userIdA, userIdB);
        Long max = Math.max(userIdA, userIdB);
        Long count = friendRelationMapper.selectCount(new LambdaQueryWrapper<FriendRelation>()
                .eq(FriendRelation::getUserIdA, min)
                .eq(FriendRelation::getUserIdB, max)
                .eq(FriendRelation::getStatus, FRIEND_ACTIVE));
        return Result.success(count > 0);
    }

    @GetMapping("/teams/{teamId}/members/check")
    public Result<Boolean> isTeamMember(@PathVariable Long teamId, @RequestParam Long userId) {
        Long count = teamMemberMapper.selectCount(new LambdaQueryWrapper<TeamMember>()
                .eq(TeamMember::getTeamId, teamId)
                .eq(TeamMember::getUserId, userId)
                .eq(TeamMember::getStatus, MEMBER_ACTIVE));
        return Result.success(count > 0);
    }

    @GetMapping("/users/{userId}/team-ids")
    public Result<List<Long>> getUserTeamIds(@PathVariable Long userId) {
        return Result.success(teamMemberMapper.selectList(new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getUserId, userId)
                        .eq(TeamMember::getStatus, MEMBER_ACTIVE))
                .stream()
                .map(TeamMember::getTeamId)
                .toList());
    }

    @GetMapping("/teams/{teamId}/member-ids")
    public Result<List<Long>> getTeamMemberIds(@PathVariable Long teamId) {
        return Result.success(teamMemberMapper.selectList(new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getTeamId, teamId)
                        .eq(TeamMember::getStatus, MEMBER_ACTIVE))
                .stream()
                .map(TeamMember::getUserId)
                .toList());
    }
}
