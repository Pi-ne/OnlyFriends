package com.ququ.social.controller.internal;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ququ.common.dto.PageResult;
import com.ququ.common.dto.UserBasicDTO;
import com.ququ.common.exception.BizException;
import com.ququ.common.response.Result;
import com.ququ.common.response.ResultCode;
import com.ququ.social.dto.request.AdminDisableTeamRequest;
import com.ququ.social.dto.request.AdminRestoreTeamRequest;
import com.ququ.social.dto.response.TeamMemberResponse;
import com.ququ.social.dto.response.TeamResponse;
import com.ququ.social.entity.Team;
import com.ququ.social.entity.TeamAdminOperationLog;
import com.ququ.social.entity.TeamMember;
import com.ququ.social.mapper.TeamAdminOperationLogMapper;
import com.ququ.social.mapper.TeamMapper;
import com.ququ.social.mapper.TeamMemberMapper;
import com.ququ.social.service.UserClient;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/admin/teams")
public class AdminTeamInternalController {
    private static final int TEAM_ACTIVE = 1;
    private static final int TEAM_DISABLED = 3;
    private static final int MEMBER_ACTIVE = 1;

    private final TeamMapper teamMapper;
    private final TeamMemberMapper teamMemberMapper;
    private final TeamAdminOperationLogMapper operationLogMapper;
    private final UserClient userClient;
    private final ObjectMapper objectMapper;

    @GetMapping
    public Result<PageResult<TeamResponse>> teams(@RequestParam(defaultValue = "1") Integer page,
                                                  @RequestParam(defaultValue = "20") Integer size,
                                                  @RequestParam(required = false) Integer status,
                                                  @RequestParam(required = false) String keyword) {
        int current = page == null || page < 1 ? 1 : page;
        int pageSize = size == null ? 20 : Math.min(Math.max(size, 1), 100);
        LambdaQueryWrapper<Team> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(status != null, Team::getStatus, status);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Team::getName, keyword).or().like(Team::getDescription, keyword));
        }
        wrapper.orderByDesc(Team::getCreatedAt);
        List<Team> all = teamMapper.selectList(wrapper);
        Map<Long, UserBasicDTO> owners = userClient.getUsersByIds(all.stream().map(Team::getOwnerId).distinct().toList());
        List<TeamResponse> rows = all.stream()
                .skip((long) (current - 1) * pageSize)
                .limit(pageSize)
                .map(team -> toTeamResponse(team, owners))
                .toList();
        return Result.success(new PageResult<>(rows, (long) all.size(), (long) current, (long) pageSize));
    }

    @GetMapping("/{id}")
    public Result<TeamResponse> teamDetail(@PathVariable Long id) {
        Team team = getTeamOrThrow(id);
        return Result.success(toTeamResponse(team, userClient.getUsersByIds(List.of(team.getOwnerId()))));
    }

    @GetMapping("/{id}/members")
    public Result<List<TeamMemberResponse>> teamMembers(@PathVariable Long id) {
        getTeamOrThrow(id);
        List<TeamMember> members = teamMemberMapper.selectList(new LambdaQueryWrapper<TeamMember>()
                .eq(TeamMember::getTeamId, id)
                .eq(TeamMember::getStatus, MEMBER_ACTIVE)
                .orderByDesc(TeamMember::getRole)
                .orderByAsc(TeamMember::getJoinedAt));
        Map<Long, UserBasicDTO> users = userClient.getUsersByIds(members.stream().map(TeamMember::getUserId).distinct().toList());
        return Result.success(members.stream().map(member -> toTeamMemberResponse(member, users.get(member.getUserId()))).toList());
    }

    @PostMapping("/{id}/disable")
    @Transactional
    public Result<Void> disable(@PathVariable Long id, @Valid @RequestBody AdminDisableTeamRequest request) {
        Team team = getTeamOrThrow(id);
        if (Integer.valueOf(TEAM_DISABLED).equals(team.getStatus())) {
            return Result.success();
        }
        team.setStatus(TEAM_DISABLED);
        teamMapper.updateById(team);
        log(id, request.getAdminId(), "DISABLE_TEAM", request.getReason().trim());
        return Result.success();
    }

    @PostMapping("/{id}/restore")
    @Transactional
    public Result<Void> restore(@PathVariable Long id, @Valid @RequestBody AdminRestoreTeamRequest request) {
        Team team = getTeamOrThrow(id);
        if (!Integer.valueOf(TEAM_DISABLED).equals(team.getStatus())) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "只有已停用小队可以恢复");
        }
        team.setStatus(TEAM_ACTIVE);
        teamMapper.updateById(team);
        log(id, request.getAdminId(), "RESTORE_TEAM", null);
        return Result.success();
    }

    private Team getTeamOrThrow(Long id) {
        Team team = teamMapper.selectById(id);
        if (team == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "小队不存在");
        }
        return team;
    }

    private void log(Long teamId, Long adminId, String operationType, String reason) {
        TeamAdminOperationLog log = new TeamAdminOperationLog();
        log.setTeamId(teamId);
        log.setAdminId(adminId);
        log.setOperationType(operationType);
        log.setReason(reason);
        operationLogMapper.insert(log);
    }

    private TeamResponse toTeamResponse(Team team, Map<Long, UserBasicDTO> owners) {
        UserBasicDTO owner = owners.get(team.getOwnerId());
        TeamResponse response = new TeamResponse();
        response.setTeamId(team.getId());
        response.setOwnerId(team.getOwnerId());
        response.setOwnerNickname(owner == null ? null : owner.getNickname());
        response.setName(team.getName());
        response.setDescription(team.getDescription());
        response.setTags(fromJson(team.getTags()));
        response.setJoinType(team.getJoinType());
        response.setMaxMembers(team.getMaxMembers());
        response.setMemberCount(team.getMemberCount());
        response.setStatus(team.getStatus());
        response.setScore(team.getScore());
        response.setCreatedAt(team.getCreatedAt());
        return response;
    }

    private TeamMemberResponse toTeamMemberResponse(TeamMember member, UserBasicDTO user) {
        TeamMemberResponse response = new TeamMemberResponse();
        response.setUserId(member.getUserId());
        if (user != null) {
            response.setNickname(user.getNickname());
            response.setAvatarUrl(user.getAvatarUrl());
            response.setUserType(user.getUserType());
        }
        response.setRole(member.getRole());
        response.setScore(member.getScore());
        response.setJoinedAt(member.getJoinedAt());
        return response;
    }

    private List<String> fromJson(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException ex) {
            return Collections.emptyList();
        }
    }
}
