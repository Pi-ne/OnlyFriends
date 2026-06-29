package com.onlyfriends.social.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyfriends.common.dto.UserBasicDTO;
import com.onlyfriends.common.exception.BizException;
import com.onlyfriends.common.response.ResultCode;
import com.onlyfriends.social.dto.request.FriendApplyRequest;
import com.onlyfriends.social.dto.request.FriendSettingRequest;
import com.onlyfriends.social.dto.request.ReviewRequest;
import com.onlyfriends.social.dto.request.TeamAlbumRequest;
import com.onlyfriends.social.dto.request.TeamAnnouncementRequest;
import com.onlyfriends.social.dto.request.TeamCreateRequest;
import com.onlyfriends.social.dto.request.TeamFileRequest;
import com.onlyfriends.social.dto.request.TeamJoinRequest;
import com.onlyfriends.social.dto.request.TeamVoteCastRequest;
import com.onlyfriends.social.dto.request.TeamVoteCreateRequest;
import com.onlyfriends.social.dto.response.FriendApplyResponse;
import com.onlyfriends.social.dto.response.TeamAlbumResponse;
import com.onlyfriends.social.dto.response.TeamAnnouncementResponse;
import com.onlyfriends.social.dto.response.TeamFileResponse;
import com.onlyfriends.social.dto.response.TeamJoinApplyResponse;
import com.onlyfriends.social.dto.response.TeamMemberResponse;
import com.onlyfriends.social.dto.response.TeamResponse;
import com.onlyfriends.social.dto.response.TeamScoreResponse;
import com.onlyfriends.social.dto.response.TeamVoteOptionResponse;
import com.onlyfriends.social.dto.response.TeamVoteResponse;
import com.onlyfriends.social.dto.response.UserRelationResponse;
import com.onlyfriends.social.entity.FriendApply;
import com.onlyfriends.social.entity.FriendRelation;
import com.onlyfriends.social.entity.Team;
import com.onlyfriends.social.entity.TeamAlbum;
import com.onlyfriends.social.entity.TeamAnnouncement;
import com.onlyfriends.social.entity.TeamFile;
import com.onlyfriends.social.entity.TeamJoinApply;
import com.onlyfriends.social.entity.TeamMember;
import com.onlyfriends.social.entity.TeamScoreLog;
import com.onlyfriends.social.entity.TeamVote;
import com.onlyfriends.social.entity.TeamVoteOption;
import com.onlyfriends.social.entity.TeamVoteRecord;
import com.onlyfriends.social.entity.UserFollow;
import com.onlyfriends.social.mapper.FriendApplyMapper;
import com.onlyfriends.social.mapper.FriendRelationMapper;
import com.onlyfriends.social.mapper.TeamAlbumMapper;
import com.onlyfriends.social.mapper.TeamAnnouncementMapper;
import com.onlyfriends.social.mapper.TeamFileMapper;
import com.onlyfriends.social.mapper.TeamJoinApplyMapper;
import com.onlyfriends.social.mapper.TeamMapper;
import com.onlyfriends.social.mapper.TeamMemberMapper;
import com.onlyfriends.social.mapper.TeamScoreLogMapper;
import com.onlyfriends.social.mapper.TeamVoteMapper;
import com.onlyfriends.social.mapper.TeamVoteOptionMapper;
import com.onlyfriends.social.mapper.TeamVoteRecordMapper;
import com.onlyfriends.social.mapper.UserFollowMapper;
import com.onlyfriends.social.service.SocialService;
import com.onlyfriends.social.service.UserClient;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SocialServiceImpl implements SocialService {
    private static final int FOLLOW_ACTIVE = 1;
    private static final int APPLY_PENDING = 0;
    private static final int APPLY_APPROVED = 1;
    private static final int APPLY_REJECTED = 2;
    private static final int FRIEND_ACTIVE = 1;
    private static final int TEAM_PUBLIC = 0;
    private static final int TEAM_ACTIVE = 1;
    private static final int TEAM_DISSOLVED = 2;
    private static final int MEMBER_NORMAL = 0;
    private static final int MEMBER_ADMIN = 1;
    private static final int MEMBER_OWNER = 2;
    private static final int MEMBER_ACTIVE = 1;
    private static final int VOTE_OPEN = 1;
    private static final int SCORE_ANNOUNCEMENT = 2;
    private static final int SCORE_ALBUM = 1;
    private static final int SCORE_FILE = 1;
    private static final int SCORE_VOTE = 2;

    private final UserFollowMapper followMapper;
    private final FriendRelationMapper friendRelationMapper;
    private final FriendApplyMapper friendApplyMapper;
    private final TeamMapper teamMapper;
    private final TeamMemberMapper teamMemberMapper;
    private final TeamJoinApplyMapper teamJoinApplyMapper;
    private final TeamAnnouncementMapper announcementMapper;
    private final TeamAlbumMapper albumMapper;
    private final TeamFileMapper fileMapper;
    private final TeamScoreLogMapper scoreLogMapper;
    private final TeamVoteMapper voteMapper;
    private final TeamVoteOptionMapper voteOptionMapper;
    private final TeamVoteRecordMapper voteRecordMapper;
    private final UserClient userClient;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void follow(Long currentUserId, Long targetUserId) {
        ensureDifferentUsers(currentUserId, targetUserId);
        ensureUserValid(targetUserId);
        if (activeFollow(currentUserId, targetUserId) != null) {
            return;
        }
        UserFollow follow = new UserFollow();
        follow.setFollowerId(currentUserId);
        follow.setFollowingId(targetUserId);
        follow.setStatus(FOLLOW_ACTIVE);
        try {
            followMapper.insert(follow);
        } catch (DuplicateKeyException ignored) {
            return;
        }
        if (activeFollow(targetUserId, currentUserId) != null) {
            ensureFriendRelation(currentUserId, targetUserId);
        }
    }

    @Override
    @Transactional
    public void unfollow(Long currentUserId, Long targetUserId) {
        UserFollow follow = activeFollow(currentUserId, targetUserId);
        if (follow != null) {
            followMapper.deleteById(follow.getId());
        }
    }

    @Override
    public List<UserRelationResponse> following(Long currentUserId) {
        List<Long> ids = followMapper.selectList(new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getFollowerId, currentUserId)
                        .eq(UserFollow::getStatus, FOLLOW_ACTIVE)
                        .orderByDesc(UserFollow::getCreatedAt))
                .stream()
                .map(UserFollow::getFollowingId)
                .toList();
        return toUserRelations(currentUserId, ids);
    }

    @Override
    public List<UserRelationResponse> followers(Long currentUserId) {
        List<Long> ids = followMapper.selectList(new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getFollowingId, currentUserId)
                        .eq(UserFollow::getStatus, FOLLOW_ACTIVE)
                        .orderByDesc(UserFollow::getCreatedAt))
                .stream()
                .map(UserFollow::getFollowerId)
                .toList();
        return toUserRelations(currentUserId, ids);
    }

    @Override
    @Transactional
    public Long applyFriend(Long currentUserId, Long targetUserId, FriendApplyRequest request) {
        ensureDifferentUsers(currentUserId, targetUserId);
        ensureUserValid(targetUserId);
        if (isFriend(currentUserId, targetUserId)) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "双方已经是好友");
        }
        FriendApply existing = friendApplyMapper.selectOne(new LambdaQueryWrapper<FriendApply>()
                .eq(FriendApply::getApplicantId, currentUserId)
                .eq(FriendApply::getTargetId, targetUserId)
                .eq(FriendApply::getStatus, APPLY_PENDING)
                .last("LIMIT 1"));
        if (existing != null) {
            return existing.getId();
        }
        FriendApply apply = new FriendApply();
        apply.setApplicantId(currentUserId);
        apply.setTargetId(targetUserId);
        apply.setMessage(request == null ? null : request.getMessage());
        apply.setStatus(APPLY_PENDING);
        friendApplyMapper.insert(apply);
        return apply.getId();
    }

    @Override
    @Transactional
    public void reviewFriendApply(Long currentUserId, Long applyId, ReviewRequest request) {
        FriendApply apply = friendApplyMapper.selectById(applyId);
        if (apply == null || !apply.getTargetId().equals(currentUserId)) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "好友申请不存在");
        }
        if (!Integer.valueOf(APPLY_PENDING).equals(apply.getStatus())) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "好友申请已处理");
        }
        if (!List.of(APPLY_APPROVED, APPLY_REJECTED).contains(request.getAction())) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "审核结果不合法");
        }
        apply.setStatus(request.getAction());
        apply.setReviewedAt(LocalDateTime.now());
        friendApplyMapper.updateById(apply);
        if (Integer.valueOf(APPLY_APPROVED).equals(request.getAction())) {
            ensureFriendRelation(apply.getApplicantId(), apply.getTargetId());
        }
    }

    @Override
    public List<FriendApplyResponse> friendApplies(Long currentUserId, String type) {
        LambdaQueryWrapper<FriendApply> wrapper = new LambdaQueryWrapper<>();
        if ("sent".equalsIgnoreCase(type)) {
            wrapper.eq(FriendApply::getApplicantId, currentUserId);
        } else {
            wrapper.eq(FriendApply::getTargetId, currentUserId);
        }
        wrapper.orderByDesc(FriendApply::getCreatedAt);
        List<FriendApply> applies = friendApplyMapper.selectList(wrapper);
        List<Long> ids = applies.stream().flatMap(apply -> List.of(apply.getApplicantId(), apply.getTargetId()).stream()).distinct().toList();
        Map<Long, UserBasicDTO> users = userClient.getUsersByIds(ids);
        return applies.stream().map(apply -> toFriendApplyResponse(apply, users)).toList();
    }

    @Override
    public List<UserRelationResponse> friends(Long currentUserId) {
        List<Long> ids = friendRelationMapper.selectList(new LambdaQueryWrapper<FriendRelation>()
                        .eq(FriendRelation::getStatus, FRIEND_ACTIVE)
                        .and(w -> w.eq(FriendRelation::getUserIdA, currentUserId).or().eq(FriendRelation::getUserIdB, currentUserId))
                        .orderByDesc(FriendRelation::getCreatedAt))
                .stream()
                .map(relation -> relation.getUserIdA().equals(currentUserId) ? relation.getUserIdB() : relation.getUserIdA())
                .toList();
        return toUserRelations(currentUserId, ids);
    }

    @Override
    @Transactional
    public void deleteFriend(Long currentUserId, Long friendUserId) {
        FriendRelation relation = activeFriendRelation(currentUserId, friendUserId);
        if (relation != null) {
            friendRelationMapper.deleteById(relation.getId());
        }
    }

    @Override
    @Transactional
    public void updateFriendSetting(Long currentUserId, Long friendUserId, FriendSettingRequest request) {
        FriendRelation relation = activeFriendRelation(currentUserId, friendUserId);
        if (relation == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "好友关系不存在");
        }
        boolean currentIsA = relation.getUserIdA().equals(currentUserId);
        String remark = trimToNull(request == null ? null : request.getRemark());
        String groupName = trimToNull(request == null ? null : request.getGroupName());
        if (currentIsA) {
            relation.setRemarkA(remark);
            relation.setGroupA(groupName);
        } else {
            relation.setRemarkB(remark);
            relation.setGroupB(groupName);
        }
        friendRelationMapper.updateById(relation);
    }

    @Override
    @Transactional
    public TeamResponse createTeam(Long currentUserId, TeamCreateRequest request) {
        ensureUserValid(currentUserId);
        Team team = new Team();
        team.setOwnerId(currentUserId);
        team.setName(request.getName().trim());
        team.setDescription(request.getDescription());
        team.setTags(toJson(request.getTags()));
        team.setJoinType(request.getJoinType());
        team.setMaxMembers(request.getMaxMembers());
        team.setMemberCount(1);
        team.setStatus(TEAM_ACTIVE);
        team.setScore(0);
        teamMapper.insert(team);

        TeamMember owner = new TeamMember();
        owner.setTeamId(team.getId());
        owner.setUserId(currentUserId);
        owner.setRole(MEMBER_OWNER);
        owner.setStatus(MEMBER_ACTIVE);
        owner.setScore(0);
        owner.setJoinedAt(LocalDateTime.now());
        teamMemberMapper.insert(owner);
        return toTeamResponse(team, userClient.getUsersByIds(List.of(currentUserId)));
    }

    @Override
    public List<TeamResponse> teams(String keyword, Long ownerId, Long joinedUserId) {
        LambdaQueryWrapper<Team> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Team::getStatus, TEAM_ACTIVE);
        wrapper.eq(ownerId != null, Team::getOwnerId, ownerId);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Team::getName, keyword).or().like(Team::getDescription, keyword));
        }
        List<Team> teams = teamMapper.selectList(wrapper.orderByDesc(Team::getCreatedAt));
        if (joinedUserId != null) {
            List<Long> joinedTeamIds = teamMemberMapper.selectList(new LambdaQueryWrapper<TeamMember>()
                            .eq(TeamMember::getUserId, joinedUserId)
                            .eq(TeamMember::getStatus, MEMBER_ACTIVE))
                    .stream()
                    .map(TeamMember::getTeamId)
                    .toList();
            teams = teams.stream().filter(team -> joinedTeamIds.contains(team.getId())).toList();
        }
        Map<Long, UserBasicDTO> owners = userClient.getUsersByIds(teams.stream().map(Team::getOwnerId).distinct().toList());
        return teams.stream().map(team -> toTeamResponse(team, owners)).toList();
    }

    @Override
    public TeamResponse teamDetail(Long currentUserId, Long teamId) {
        Team team = getActiveTeamOrThrow(teamId);
        TeamResponse response = toTeamResponse(team, userClient.getUsersByIds(List.of(team.getOwnerId())));
        if (currentUserId != null) {
            TeamMember member = activeTeamMember(teamId, currentUserId);
            response.setJoined(member != null);
            response.setMyRole(member == null ? null : member.getRole());
        } else {
            response.setJoined(false);
        }
        return response;
    }

    @Override
    @Transactional
    public Long joinTeam(Long currentUserId, Long teamId, TeamJoinRequest request) {
        Team team = getActiveTeamOrThrow(teamId);
        ensureUserValid(currentUserId);
        if (activeTeamMember(teamId, currentUserId) != null) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "已经是小队成员");
        }
        if (team.getMemberCount() >= team.getMaxMembers()) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "小队人数已满");
        }
        if (Integer.valueOf(TEAM_PUBLIC).equals(team.getJoinType())) {
            addTeamMember(team, currentUserId, MEMBER_NORMAL);
            return null;
        }
        TeamJoinApply existing = teamJoinApplyMapper.selectOne(new LambdaQueryWrapper<TeamJoinApply>()
                .eq(TeamJoinApply::getTeamId, teamId)
                .eq(TeamJoinApply::getUserId, currentUserId)
                .eq(TeamJoinApply::getStatus, APPLY_PENDING)
                .last("LIMIT 1"));
        if (existing != null) {
            return existing.getId();
        }
        TeamJoinApply apply = new TeamJoinApply();
        apply.setTeamId(teamId);
        apply.setUserId(currentUserId);
        apply.setMessage(request == null ? null : request.getMessage());
        apply.setStatus(APPLY_PENDING);
        teamJoinApplyMapper.insert(apply);
        return apply.getId();
    }

    @Override
    public List<TeamJoinApplyResponse> teamJoinApplies(Long currentUserId, Long teamId) {
        ensureTeamManager(currentUserId, teamId);
        List<TeamJoinApply> applies = teamJoinApplyMapper.selectList(new LambdaQueryWrapper<TeamJoinApply>()
                .eq(TeamJoinApply::getTeamId, teamId)
                .orderByDesc(TeamJoinApply::getCreatedAt));
        Map<Long, UserBasicDTO> users = userClient.getUsersByIds(applies.stream().map(TeamJoinApply::getUserId).distinct().toList());
        return applies.stream().map(apply -> toTeamJoinApplyResponse(apply, users.get(apply.getUserId()))).toList();
    }

    @Override
    @Transactional
    public void reviewTeamJoinApply(Long currentUserId, Long teamId, Long applyId, ReviewRequest request) {
        ensureTeamManager(currentUserId, teamId);
        TeamJoinApply apply = teamJoinApplyMapper.selectById(applyId);
        if (apply == null || !apply.getTeamId().equals(teamId)) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "加入申请不存在");
        }
        if (!Integer.valueOf(APPLY_PENDING).equals(apply.getStatus())) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "加入申请已处理");
        }
        if (!List.of(APPLY_APPROVED, APPLY_REJECTED).contains(request.getAction())) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "审核结果不合法");
        }
        if (Integer.valueOf(APPLY_REJECTED).equals(request.getAction()) && !StringUtils.hasText(request.getReason())) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "拒绝必须填写原因");
        }
        Team team = getActiveTeamOrThrow(teamId);
        if (Integer.valueOf(APPLY_APPROVED).equals(request.getAction())) {
            if (team.getMemberCount() >= team.getMaxMembers()) {
                throw new BizException(ResultCode.BAD_REQUEST.getCode(), "小队人数已满");
            }
            addTeamMember(team, apply.getUserId(), MEMBER_NORMAL);
        }
        apply.setStatus(request.getAction());
        apply.setReviewerId(currentUserId);
        apply.setRejectReason(Integer.valueOf(APPLY_REJECTED).equals(request.getAction()) ? request.getReason().trim() : null);
        apply.setReviewedAt(LocalDateTime.now());
        teamJoinApplyMapper.updateById(apply);
    }

    @Override
    public List<TeamMemberResponse> teamMembers(Long teamId) {
        getActiveTeamOrThrow(teamId);
        List<TeamMember> members = teamMemberMapper.selectList(new LambdaQueryWrapper<TeamMember>()
                .eq(TeamMember::getTeamId, teamId)
                .eq(TeamMember::getStatus, MEMBER_ACTIVE)
                .orderByDesc(TeamMember::getRole)
                .orderByAsc(TeamMember::getJoinedAt));
        Map<Long, UserBasicDTO> users = userClient.getUsersByIds(members.stream().map(TeamMember::getUserId).distinct().toList());
        return members.stream().map(member -> toTeamMemberResponse(member, users.get(member.getUserId()))).toList();
    }

    @Override
    @Transactional
    public void leaveTeam(Long currentUserId, Long teamId) {
        Team team = getActiveTeamOrThrow(teamId);
        TeamMember member = requireActiveTeamMember(teamId, currentUserId);
        if (Integer.valueOf(MEMBER_OWNER).equals(member.getRole())) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "队长不能直接退出，请先转让队长");
        }
        deactivateTeamMember(team, member);
    }

    @Override
    @Transactional
    public void removeTeamMember(Long currentUserId, Long teamId, Long memberUserId) {
        ensureDifferentUsers(currentUserId, memberUserId);
        Team team = getActiveTeamOrThrow(teamId);
        TeamMember operator = ensureTeamManager(currentUserId, teamId);
        TeamMember target = requireActiveTeamMember(teamId, memberUserId);
        if (Integer.valueOf(MEMBER_OWNER).equals(target.getRole())) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "不能移除队长");
        }
        if (Integer.valueOf(MEMBER_ADMIN).equals(target.getRole()) && !Integer.valueOf(MEMBER_OWNER).equals(operator.getRole())) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "只有队长可以移除管理员");
        }
        deactivateTeamMember(team, target);
    }

    @Override
    @Transactional
    public void setTeamAdmin(Long currentUserId, Long teamId, Long memberUserId) {
        ensureTeamOwner(currentUserId, teamId);
        TeamMember member = requireActiveTeamMember(teamId, memberUserId);
        if (Integer.valueOf(MEMBER_OWNER).equals(member.getRole())) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "队长不需要设置为管理员");
        }
        if (!Integer.valueOf(MEMBER_ADMIN).equals(member.getRole())) {
            member.setRole(MEMBER_ADMIN);
            teamMemberMapper.updateById(member);
        }
    }

    @Override
    @Transactional
    public void unsetTeamAdmin(Long currentUserId, Long teamId, Long memberUserId) {
        ensureTeamOwner(currentUserId, teamId);
        TeamMember member = requireActiveTeamMember(teamId, memberUserId);
        if (!Integer.valueOf(MEMBER_ADMIN).equals(member.getRole())) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "该成员不是管理员");
        }
        member.setRole(MEMBER_NORMAL);
        teamMemberMapper.updateById(member);
    }

    @Override
    @Transactional
    public void transferTeamOwner(Long currentUserId, Long teamId, Long newOwnerUserId) {
        ensureDifferentUsers(currentUserId, newOwnerUserId);
        Team team = getActiveTeamOrThrow(teamId);
        TeamMember currentOwner = ensureTeamOwner(currentUserId, teamId);
        TeamMember newOwner = requireActiveTeamMember(teamId, newOwnerUserId);
        currentOwner.setRole(MEMBER_ADMIN);
        newOwner.setRole(MEMBER_OWNER);
        teamMemberMapper.updateById(currentOwner);
        teamMemberMapper.updateById(newOwner);
        team.setOwnerId(newOwnerUserId);
        teamMapper.updateById(team);
    }

    @Override
    @Transactional
    public TeamAnnouncementResponse publishAnnouncement(Long currentUserId, Long teamId, TeamAnnouncementRequest request) {
        ensureTeamManager(currentUserId, teamId);
        TeamAnnouncement announcement = new TeamAnnouncement();
        announcement.setTeamId(teamId);
        announcement.setPublisherId(currentUserId);
        announcement.setTitle(request.getTitle().trim());
        announcement.setContent(request.getContent().trim());
        announcementMapper.insert(announcement);
        addScore(teamId, currentUserId, SCORE_ANNOUNCEMENT, "announcement", announcement.getId());
        return toAnnouncementResponse(announcement, userClient.getUsersByIds(List.of(currentUserId)).get(currentUserId));
    }

    @Override
    public List<TeamAnnouncementResponse> announcements(Long teamId) {
        getActiveTeamOrThrow(teamId);
        List<TeamAnnouncement> announcements = announcementMapper.selectList(new LambdaQueryWrapper<TeamAnnouncement>()
                .eq(TeamAnnouncement::getTeamId, teamId)
                .orderByDesc(TeamAnnouncement::getCreatedAt));
        Map<Long, UserBasicDTO> users = userClient.getUsersByIds(announcements.stream()
                .map(TeamAnnouncement::getPublisherId)
                .distinct()
                .toList());
        return announcements.stream()
                .map(announcement -> toAnnouncementResponse(announcement, users.get(announcement.getPublisherId())))
                .toList();
    }

    @Override
    @Transactional
    public TeamAlbumResponse addAlbumImage(Long currentUserId, Long teamId, TeamAlbumRequest request) {
        requireActiveTeamMember(teamId, currentUserId);
        TeamAlbum album = new TeamAlbum();
        album.setTeamId(teamId);
        album.setUserId(currentUserId);
        album.setImageUrl(request.getImageUrl().trim());
        album.setDescription(StringUtils.hasText(request.getDescription()) ? request.getDescription().trim() : null);
        albumMapper.insert(album);
        addScore(teamId, currentUserId, SCORE_ALBUM, "album", album.getId());
        return toAlbumResponse(album, userClient.getUsersByIds(List.of(currentUserId)).get(currentUserId));
    }

    @Override
    public List<TeamAlbumResponse> album(Long teamId) {
        getActiveTeamOrThrow(teamId);
        List<TeamAlbum> albums = albumMapper.selectList(new LambdaQueryWrapper<TeamAlbum>()
                .eq(TeamAlbum::getTeamId, teamId)
                .orderByDesc(TeamAlbum::getCreatedAt));
        Map<Long, UserBasicDTO> users = userClient.getUsersByIds(albums.stream()
                .map(TeamAlbum::getUserId)
                .distinct()
                .toList());
        return albums.stream()
                .map(album -> toAlbumResponse(album, users.get(album.getUserId())))
                .toList();
    }

    @Override
    @Transactional
    public TeamFileResponse addTeamFile(Long currentUserId, Long teamId, TeamFileRequest request) {
        requireActiveTeamMember(teamId, currentUserId);
        TeamFile file = new TeamFile();
        file.setTeamId(teamId);
        file.setUserId(currentUserId);
        file.setFileName(request.getFileName().trim());
        file.setFileUrl(request.getFileUrl().trim());
        file.setFileSize(request.getFileSize() == null ? 0L : request.getFileSize());
        fileMapper.insert(file);
        addScore(teamId, currentUserId, SCORE_FILE, "file", file.getId());
        return toFileResponse(file, userClient.getUsersByIds(List.of(currentUserId)).get(currentUserId));
    }

    @Override
    public List<TeamFileResponse> files(Long teamId) {
        getActiveTeamOrThrow(teamId);
        List<TeamFile> files = fileMapper.selectList(new LambdaQueryWrapper<TeamFile>()
                .eq(TeamFile::getTeamId, teamId)
                .orderByDesc(TeamFile::getCreatedAt));
        Map<Long, UserBasicDTO> users = userClient.getUsersByIds(files.stream()
                .map(TeamFile::getUserId)
                .distinct()
                .toList());
        return files.stream()
                .map(file -> toFileResponse(file, users.get(file.getUserId())))
                .toList();
    }

    @Override
    public List<TeamScoreResponse> scores(Long teamId) {
        getActiveTeamOrThrow(teamId);
        List<TeamMember> members = teamMemberMapper.selectList(new LambdaQueryWrapper<TeamMember>()
                .eq(TeamMember::getTeamId, teamId)
                .eq(TeamMember::getStatus, MEMBER_ACTIVE)
                .orderByDesc(TeamMember::getScore)
                .orderByAsc(TeamMember::getJoinedAt));
        Map<Long, UserBasicDTO> users = userClient.getUsersByIds(members.stream()
                .map(TeamMember::getUserId)
                .distinct()
                .toList());
        return members.stream()
                .map(member -> toScoreResponse(member, users.get(member.getUserId())))
                .toList();
    }

    @Override
    @Transactional
    public TeamVoteResponse createVote(Long currentUserId, Long teamId, TeamVoteCreateRequest request) {
        requireActiveTeamMember(teamId, currentUserId);
        List<String> options = request.getOptions().stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (options.size() < 2) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "投票至少需要两个不同选项");
        }
        TeamVote vote = new TeamVote();
        vote.setTeamId(teamId);
        vote.setCreatorId(currentUserId);
        vote.setTitle(request.getTitle().trim());
        vote.setDescription(StringUtils.hasText(request.getDescription()) ? request.getDescription().trim() : null);
        vote.setMultiple(Boolean.TRUE.equals(request.getMultiple()) ? 1 : 0);
        vote.setDeadline(request.getDeadline());
        vote.setStatus(VOTE_OPEN);
        voteMapper.insert(vote);

        int sortOrder = 0;
        List<TeamVoteOption> savedOptions = new ArrayList<>();
        for (String content : options) {
            TeamVoteOption option = new TeamVoteOption();
            option.setVoteId(vote.getId());
            option.setContent(content);
            option.setVoteCount(0);
            option.setSortOrder(sortOrder++);
            voteOptionMapper.insert(option);
            savedOptions.add(option);
        }
        addScore(teamId, currentUserId, SCORE_VOTE, "vote", vote.getId());
        return toVoteResponse(vote, savedOptions, Collections.emptySet(), userClient.getUsersByIds(List.of(currentUserId)).get(currentUserId));
    }

    @Override
    public List<TeamVoteResponse> votes(Long currentUserId, Long teamId) {
        getActiveTeamOrThrow(teamId);
        List<TeamVote> votes = voteMapper.selectList(new LambdaQueryWrapper<TeamVote>()
                .eq(TeamVote::getTeamId, teamId)
                .orderByDesc(TeamVote::getCreatedAt));
        Map<Long, UserBasicDTO> users = userClient.getUsersByIds(votes.stream()
                .map(TeamVote::getCreatorId)
                .distinct()
                .toList());
        return votes.stream()
                .map(vote -> toVoteResponse(vote, voteOptions(vote.getId()), selectedOptionIds(vote.getId(), currentUserId), users.get(vote.getCreatorId())))
                .toList();
    }

    @Override
    @Transactional
    public TeamVoteResponse castVote(Long currentUserId, Long teamId, Long voteId, TeamVoteCastRequest request) {
        requireActiveTeamMember(teamId, currentUserId);
        TeamVote vote = voteMapper.selectById(voteId);
        if (vote == null || !teamId.equals(vote.getTeamId())) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "投票不存在");
        }
        if (!Integer.valueOf(VOTE_OPEN).equals(vote.getStatus())) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "投票已关闭");
        }
        if (vote.getDeadline() != null && vote.getDeadline().isBefore(LocalDateTime.now())) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "投票已截止");
        }
        if (!selectedOptionIds(voteId, currentUserId).isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "不能重复投票");
        }
        Set<Long> selectedIds = new LinkedHashSet<>(request.getOptionIds());
        if (selectedIds.isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "投票选项不能为空");
        }
        if (!Integer.valueOf(1).equals(vote.getMultiple()) && selectedIds.size() > 1) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "该投票只能选择一个选项");
        }
        List<TeamVoteOption> options = voteOptions(voteId);
        Map<Long, TeamVoteOption> optionMap = options.stream().collect(java.util.stream.Collectors.toMap(TeamVoteOption::getId, option -> option));
        for (Long optionId : selectedIds) {
            TeamVoteOption option = optionMap.get(optionId);
            if (option == null) {
                throw new BizException(ResultCode.BAD_REQUEST.getCode(), "投票选项不存在");
            }
            TeamVoteRecord record = new TeamVoteRecord();
            record.setVoteId(voteId);
            record.setOptionId(optionId);
            record.setUserId(currentUserId);
            voteRecordMapper.insert(record);
            option.setVoteCount(option.getVoteCount() + 1);
            voteOptionMapper.updateById(option);
        }
        return toVoteResponse(vote, voteOptions(voteId), selectedIds, userClient.getUsersByIds(List.of(vote.getCreatorId())).get(vote.getCreatorId()));
    }

    @Override
    @Transactional
    public void dissolveTeam(Long currentUserId, Long teamId) {
        Team team = getActiveTeamOrThrow(teamId);
        if (!team.getOwnerId().equals(currentUserId)) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "只有队长可以解散小队");
        }
        team.setStatus(TEAM_DISSOLVED);
        teamMapper.updateById(team);
    }

    private void addTeamMember(Team team, Long userId, Integer role) {
        TeamMember existing = teamMemberMapper.selectOne(new LambdaQueryWrapper<TeamMember>()
                .eq(TeamMember::getTeamId, team.getId())
                .eq(TeamMember::getUserId, userId)
                .last("LIMIT 1"));
        if (existing != null) {
            if (Integer.valueOf(MEMBER_ACTIVE).equals(existing.getStatus())) {
                throw new BizException(ResultCode.BAD_REQUEST.getCode(), "已经是小队成员");
            }
            existing.setRole(role);
            existing.setStatus(MEMBER_ACTIVE);
            existing.setJoinedAt(LocalDateTime.now());
            teamMemberMapper.updateById(existing);
            team.setMemberCount(team.getMemberCount() + 1);
            teamMapper.updateById(team);
            return;
        }
        TeamMember member = new TeamMember();
        member.setTeamId(team.getId());
        member.setUserId(userId);
        member.setRole(role);
        member.setStatus(MEMBER_ACTIVE);
        member.setScore(0);
        member.setJoinedAt(LocalDateTime.now());
        try {
            teamMemberMapper.insert(member);
        } catch (DuplicateKeyException ex) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "已经是小队成员");
        }
        team.setMemberCount(team.getMemberCount() + 1);
        teamMapper.updateById(team);
    }

    private void ensureFriendRelation(Long userIdA, Long userIdB) {
        if (activeFriendRelation(userIdA, userIdB) != null) {
            return;
        }
        Long min = Math.min(userIdA, userIdB);
        Long max = Math.max(userIdA, userIdB);
        FriendRelation relation = new FriendRelation();
        relation.setUserIdA(min);
        relation.setUserIdB(max);
        relation.setStatus(FRIEND_ACTIVE);
        try {
            friendRelationMapper.insert(relation);
        } catch (DuplicateKeyException ignored) {
        }
    }

    private UserFollow activeFollow(Long followerId, Long followingId) {
        return followMapper.selectOne(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, followerId)
                .eq(UserFollow::getFollowingId, followingId)
                .eq(UserFollow::getStatus, FOLLOW_ACTIVE)
                .last("LIMIT 1"));
    }

    private FriendRelation activeFriendRelation(Long userIdA, Long userIdB) {
        Long min = Math.min(userIdA, userIdB);
        Long max = Math.max(userIdA, userIdB);
        return friendRelationMapper.selectOne(new LambdaQueryWrapper<FriendRelation>()
                .eq(FriendRelation::getUserIdA, min)
                .eq(FriendRelation::getUserIdB, max)
                .eq(FriendRelation::getStatus, FRIEND_ACTIVE)
                .last("LIMIT 1"));
    }

    private boolean isFriend(Long userIdA, Long userIdB) {
        return activeFriendRelation(userIdA, userIdB) != null;
    }

    private TeamMember activeTeamMember(Long teamId, Long userId) {
        return teamMemberMapper.selectOne(new LambdaQueryWrapper<TeamMember>()
                .eq(TeamMember::getTeamId, teamId)
                .eq(TeamMember::getUserId, userId)
                .eq(TeamMember::getStatus, MEMBER_ACTIVE)
                .last("LIMIT 1"));
    }

    private TeamMember requireActiveTeamMember(Long teamId, Long userId) {
        getActiveTeamOrThrow(teamId);
        TeamMember member = activeTeamMember(teamId, userId);
        if (member == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "小队成员不存在");
        }
        return member;
    }

    private void deactivateTeamMember(Team team, TeamMember member) {
        member.setStatus(0);
        teamMemberMapper.updateById(member);
        team.setMemberCount(Math.max(0, team.getMemberCount() - 1));
        teamMapper.updateById(team);
    }

    private Team getActiveTeamOrThrow(Long teamId) {
        Team team = teamMapper.selectById(teamId);
        if (team == null || !Integer.valueOf(TEAM_ACTIVE).equals(team.getStatus())) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "小队不存在或已解散");
        }
        return team;
    }

    private TeamMember ensureTeamOwner(Long currentUserId, Long teamId) {
        Team team = getActiveTeamOrThrow(teamId);
        if (!team.getOwnerId().equals(currentUserId)) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "只有队长可以操作");
        }
        TeamMember member = requireActiveTeamMember(teamId, currentUserId);
        if (!Integer.valueOf(MEMBER_OWNER).equals(member.getRole())) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "只有队长可以操作");
        }
        return member;
    }

    private TeamMember ensureTeamManager(Long currentUserId, Long teamId) {
        getActiveTeamOrThrow(teamId);
        TeamMember member = requireActiveTeamMember(teamId, currentUserId);
        if (!List.of(MEMBER_ADMIN, MEMBER_OWNER).contains(member.getRole())) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "只有队长或管理员可以操作");
        }
        return member;
    }

    private void ensureDifferentUsers(Long currentUserId, Long targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "不能对自己执行该操作");
        }
    }

    private void ensureUserValid(Long userId) {
        if (!userClient.isUserValid(userId)) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "用户不存在或状态异常");
        }
    }

    private List<UserRelationResponse> toUserRelations(Long currentUserId, List<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, UserBasicDTO> users = userClient.getUsersByIds(ids);
        return ids.stream().map(id -> {
            UserBasicDTO user = users.get(id);
            UserRelationResponse response = new UserRelationResponse();
            response.setUserId(id);
            if (user != null) {
                response.setNickname(user.getNickname());
                response.setAvatarUrl(user.getAvatarUrl());
                response.setUserType(user.getUserType());
            }
            response.setMutualFollow(activeFollow(currentUserId, id) != null && activeFollow(id, currentUserId) != null);
            response.setFriend(isFriend(currentUserId, id));
            FriendRelation relation = activeFriendRelation(currentUserId, id);
            if (relation != null) {
                boolean currentIsA = relation.getUserIdA().equals(currentUserId);
                response.setRemark(currentIsA ? relation.getRemarkA() : relation.getRemarkB());
                response.setGroupName(currentIsA ? relation.getGroupA() : relation.getGroupB());
            }
            return response;
        }).toList();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private FriendApplyResponse toFriendApplyResponse(FriendApply apply, Map<Long, UserBasicDTO> users) {
        FriendApplyResponse response = new FriendApplyResponse();
        response.setApplyId(apply.getId());
        response.setApplicantId(apply.getApplicantId());
        response.setTargetId(apply.getTargetId());
        response.setApplicantNickname(nickname(users.get(apply.getApplicantId())));
        response.setTargetNickname(nickname(users.get(apply.getTargetId())));
        response.setMessage(apply.getMessage());
        response.setStatus(apply.getStatus());
        response.setReviewedAt(apply.getReviewedAt());
        response.setCreatedAt(apply.getCreatedAt());
        return response;
    }

    private TeamResponse toTeamResponse(Team team, Map<Long, UserBasicDTO> owners) {
        TeamResponse response = new TeamResponse();
        response.setTeamId(team.getId());
        response.setOwnerId(team.getOwnerId());
        response.setOwnerNickname(nickname(owners.get(team.getOwnerId())));
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

    private TeamJoinApplyResponse toTeamJoinApplyResponse(TeamJoinApply apply, UserBasicDTO user) {
        TeamJoinApplyResponse response = new TeamJoinApplyResponse();
        response.setApplyId(apply.getId());
        response.setTeamId(apply.getTeamId());
        response.setUserId(apply.getUserId());
        response.setNickname(nickname(user));
        response.setMessage(apply.getMessage());
        response.setStatus(apply.getStatus());
        response.setRejectReason(apply.getRejectReason());
        response.setReviewedAt(apply.getReviewedAt());
        response.setCreatedAt(apply.getCreatedAt());
        return response;
    }

    private TeamAnnouncementResponse toAnnouncementResponse(TeamAnnouncement announcement, UserBasicDTO user) {
        TeamAnnouncementResponse response = new TeamAnnouncementResponse();
        response.setAnnouncementId(announcement.getId());
        response.setTeamId(announcement.getTeamId());
        response.setPublisherId(announcement.getPublisherId());
        response.setPublisherNickname(nickname(user));
        response.setTitle(announcement.getTitle());
        response.setContent(announcement.getContent());
        response.setCreatedAt(announcement.getCreatedAt());
        return response;
    }

    private TeamAlbumResponse toAlbumResponse(TeamAlbum album, UserBasicDTO user) {
        TeamAlbumResponse response = new TeamAlbumResponse();
        response.setAlbumId(album.getId());
        response.setTeamId(album.getTeamId());
        response.setUserId(album.getUserId());
        response.setNickname(nickname(user));
        response.setImageUrl(album.getImageUrl());
        response.setDescription(album.getDescription());
        response.setCreatedAt(album.getCreatedAt());
        return response;
    }

    private TeamFileResponse toFileResponse(TeamFile file, UserBasicDTO user) {
        TeamFileResponse response = new TeamFileResponse();
        response.setFileId(file.getId());
        response.setTeamId(file.getTeamId());
        response.setUserId(file.getUserId());
        response.setNickname(nickname(user));
        response.setFileName(file.getFileName());
        response.setFileUrl(file.getFileUrl());
        response.setFileSize(file.getFileSize());
        response.setCreatedAt(file.getCreatedAt());
        return response;
    }

    private TeamScoreResponse toScoreResponse(TeamMember member, UserBasicDTO user) {
        TeamScoreResponse response = new TeamScoreResponse();
        response.setUserId(member.getUserId());
        if (user != null) {
            response.setNickname(user.getNickname());
            response.setAvatarUrl(user.getAvatarUrl());
        }
        response.setRole(member.getRole());
        response.setScore(member.getScore());
        return response;
    }

    private TeamVoteResponse toVoteResponse(TeamVote vote,
                                            List<TeamVoteOption> options,
                                            Set<Long> selectedOptionIds,
                                            UserBasicDTO creator) {
        TeamVoteResponse response = new TeamVoteResponse();
        response.setVoteId(vote.getId());
        response.setTeamId(vote.getTeamId());
        response.setCreatorId(vote.getCreatorId());
        response.setCreatorNickname(nickname(creator));
        response.setTitle(vote.getTitle());
        response.setDescription(vote.getDescription());
        response.setMultiple(Integer.valueOf(1).equals(vote.getMultiple()));
        response.setStatus(vote.getStatus());
        response.setDeadline(vote.getDeadline());
        response.setCreatedAt(vote.getCreatedAt());
        response.setOptions(options.stream().map(option -> {
            TeamVoteOptionResponse optionResponse = new TeamVoteOptionResponse();
            optionResponse.setOptionId(option.getId());
            optionResponse.setContent(option.getContent());
            optionResponse.setVoteCount(option.getVoteCount());
            optionResponse.setSelected(selectedOptionIds.contains(option.getId()));
            return optionResponse;
        }).toList());
        return response;
    }

    private List<TeamVoteOption> voteOptions(Long voteId) {
        return voteOptionMapper.selectList(new LambdaQueryWrapper<TeamVoteOption>()
                .eq(TeamVoteOption::getVoteId, voteId)
                .orderByAsc(TeamVoteOption::getSortOrder));
    }

    private Set<Long> selectedOptionIds(Long voteId, Long userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        return voteRecordMapper.selectList(new LambdaQueryWrapper<TeamVoteRecord>()
                        .eq(TeamVoteRecord::getVoteId, voteId)
                        .eq(TeamVoteRecord::getUserId, userId))
                .stream()
                .map(TeamVoteRecord::getOptionId)
                .collect(java.util.stream.Collectors.toSet());
    }

    private void addScore(Long teamId, Long userId, Integer scoreChange, String actionType, Long relatedId) {
        TeamMember member = requireActiveTeamMember(teamId, userId);
        member.setScore(member.getScore() + scoreChange);
        teamMemberMapper.updateById(member);

        Team team = getActiveTeamOrThrow(teamId);
        team.setScore(team.getScore() + scoreChange);
        teamMapper.updateById(team);

        TeamScoreLog log = new TeamScoreLog();
        log.setTeamId(teamId);
        log.setUserId(userId);
        log.setScoreChange(scoreChange);
        log.setActionType(actionType);
        log.setRelatedId(relatedId);
        scoreLogMapper.insert(log);
    }

    private String nickname(UserBasicDTO user) {
        return user == null ? null : user.getNickname();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Collections.emptyList() : value);
        } catch (JsonProcessingException ex) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "JSON字段格式错误");
        }
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
