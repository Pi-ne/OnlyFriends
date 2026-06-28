package com.ququ.social.service;

import com.ququ.social.dto.request.FriendApplyRequest;
import com.ququ.social.dto.request.FriendSettingRequest;
import com.ququ.social.dto.request.ReviewRequest;
import com.ququ.social.dto.request.TeamAlbumRequest;
import com.ququ.social.dto.request.TeamAnnouncementRequest;
import com.ququ.social.dto.request.TeamCreateRequest;
import com.ququ.social.dto.request.TeamFileRequest;
import com.ququ.social.dto.request.TeamJoinRequest;
import com.ququ.social.dto.request.TeamVoteCastRequest;
import com.ququ.social.dto.request.TeamVoteCreateRequest;
import com.ququ.social.dto.response.FriendApplyResponse;
import com.ququ.social.dto.response.TeamAlbumResponse;
import com.ququ.social.dto.response.TeamAnnouncementResponse;
import com.ququ.social.dto.response.TeamFileResponse;
import com.ququ.social.dto.response.TeamJoinApplyResponse;
import com.ququ.social.dto.response.TeamMemberResponse;
import com.ququ.social.dto.response.TeamResponse;
import com.ququ.social.dto.response.TeamScoreResponse;
import com.ququ.social.dto.response.TeamVoteResponse;
import com.ququ.social.dto.response.UserRelationResponse;

import java.util.List;

public interface SocialService {
    void follow(Long currentUserId, Long targetUserId);

    void unfollow(Long currentUserId, Long targetUserId);

    List<UserRelationResponse> following(Long currentUserId);

    List<UserRelationResponse> followers(Long currentUserId);

    Long applyFriend(Long currentUserId, Long targetUserId, FriendApplyRequest request);

    void reviewFriendApply(Long currentUserId, Long applyId, ReviewRequest request);

    List<FriendApplyResponse> friendApplies(Long currentUserId, String type);

    List<UserRelationResponse> friends(Long currentUserId);

    void deleteFriend(Long currentUserId, Long friendUserId);

    void updateFriendSetting(Long currentUserId, Long friendUserId, FriendSettingRequest request);

    TeamResponse createTeam(Long currentUserId, TeamCreateRequest request);

    List<TeamResponse> teams(String keyword, Long ownerId, Long joinedUserId);

    TeamResponse teamDetail(Long currentUserId, Long teamId);

    Long joinTeam(Long currentUserId, Long teamId, TeamJoinRequest request);

    List<TeamJoinApplyResponse> teamJoinApplies(Long currentUserId, Long teamId);

    void reviewTeamJoinApply(Long currentUserId, Long teamId, Long applyId, ReviewRequest request);

    List<TeamMemberResponse> teamMembers(Long teamId);

    void leaveTeam(Long currentUserId, Long teamId);

    void removeTeamMember(Long currentUserId, Long teamId, Long memberUserId);

    void setTeamAdmin(Long currentUserId, Long teamId, Long memberUserId);

    void unsetTeamAdmin(Long currentUserId, Long teamId, Long memberUserId);

    void transferTeamOwner(Long currentUserId, Long teamId, Long newOwnerUserId);

    void dissolveTeam(Long currentUserId, Long teamId);

    TeamAnnouncementResponse publishAnnouncement(Long currentUserId, Long teamId, TeamAnnouncementRequest request);

    List<TeamAnnouncementResponse> announcements(Long teamId);

    TeamAlbumResponse addAlbumImage(Long currentUserId, Long teamId, TeamAlbumRequest request);

    List<TeamAlbumResponse> album(Long teamId);

    TeamFileResponse addTeamFile(Long currentUserId, Long teamId, TeamFileRequest request);

    List<TeamFileResponse> files(Long teamId);

    List<TeamScoreResponse> scores(Long teamId);

    TeamVoteResponse createVote(Long currentUserId, Long teamId, TeamVoteCreateRequest request);

    List<TeamVoteResponse> votes(Long currentUserId, Long teamId);

    TeamVoteResponse castVote(Long currentUserId, Long teamId, Long voteId, TeamVoteCastRequest request);
}
