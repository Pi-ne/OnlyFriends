package com.onlyfriends.social;

import com.onlyfriends.common.dto.UserBasicDTO;
import com.onlyfriends.common.exception.BizException;
import com.onlyfriends.social.dto.request.ReviewRequest;
import com.onlyfriends.social.dto.request.TeamAlbumRequest;
import com.onlyfriends.social.dto.request.TeamAnnouncementRequest;
import com.onlyfriends.social.dto.request.TeamCreateRequest;
import com.onlyfriends.social.dto.request.TeamFileRequest;
import com.onlyfriends.social.dto.request.TeamVoteCastRequest;
import com.onlyfriends.social.dto.request.TeamVoteCreateRequest;
import com.onlyfriends.social.dto.response.TeamAnnouncementResponse;
import com.onlyfriends.social.dto.response.TeamMemberResponse;
import com.onlyfriends.social.dto.response.TeamResponse;
import com.onlyfriends.social.dto.response.TeamScoreResponse;
import com.onlyfriends.social.dto.response.TeamVoteResponse;
import com.onlyfriends.social.service.SocialService;
import com.onlyfriends.social.service.UserClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest
class SocialServiceTest {
    @Autowired
    private SocialService socialService;

    @MockBean
    private UserClient userClient;

    @Test
    void teamDetailLeaveAndRejoinWork() {
        mockUsers();
        TeamResponse team = socialService.createTeam(10001L, teamRequest("城市徒步小队", 0));

        assertThat(socialService.teamDetail(10001L, team.getTeamId()).getMyRole()).isEqualTo(2);

        socialService.joinTeam(20001L, team.getTeamId(), null);
        TeamResponse joinedDetail = socialService.teamDetail(20001L, team.getTeamId());
        assertThat(joinedDetail.getJoined()).isTrue();
        assertThat(joinedDetail.getMyRole()).isZero();

        socialService.leaveTeam(20001L, team.getTeamId());
        assertThat(socialService.teamDetail(20001L, team.getTeamId()).getJoined()).isFalse();
        assertThat(socialService.teamMembers(team.getTeamId())).extracting(TeamMemberResponse::getUserId)
                .containsExactly(10001L);

        socialService.joinTeam(20001L, team.getTeamId(), null);
        assertThat(socialService.teamMembers(team.getTeamId())).extracting(TeamMemberResponse::getUserId)
                .containsExactly(10001L, 20001L);
        assertThatThrownBy(() -> socialService.leaveTeam(10001L, team.getTeamId()))
                .isInstanceOf(BizException.class);
    }

    @Test
    void teamAdminReviewRemoveAndTransferOwnerWork() {
        mockUsers();
        TeamResponse team = socialService.createTeam(10001L, teamRequest("读书会小队", 1));

        Long firstApplyId = socialService.joinTeam(20001L, team.getTeamId(), null);
        socialService.reviewTeamJoinApply(10001L, team.getTeamId(), firstApplyId, approve());
        socialService.setTeamAdmin(10001L, team.getTeamId(), 20001L);
        assertThat(socialService.teamDetail(20001L, team.getTeamId()).getMyRole()).isEqualTo(1);

        Long secondApplyId = socialService.joinTeam(30001L, team.getTeamId(), null);
        socialService.reviewTeamJoinApply(20001L, team.getTeamId(), secondApplyId, approve());
        assertThat(socialService.teamMembers(team.getTeamId())).extracting(TeamMemberResponse::getUserId)
                .containsExactly(10001L, 20001L, 30001L);

        socialService.removeTeamMember(20001L, team.getTeamId(), 30001L);
        assertThat(socialService.teamMembers(team.getTeamId())).extracting(TeamMemberResponse::getUserId)
                .containsExactly(10001L, 20001L);
        assertThatThrownBy(() -> socialService.removeTeamMember(20001L, team.getTeamId(), 10001L))
                .isInstanceOf(BizException.class);

        socialService.transferTeamOwner(10001L, team.getTeamId(), 20001L);
        TeamResponse newOwnerDetail = socialService.teamDetail(20001L, team.getTeamId());
        assertThat(newOwnerDetail.getOwnerId()).isEqualTo(20001L);
        assertThat(newOwnerDetail.getMyRole()).isEqualTo(2);
        assertThat(socialService.teamDetail(10001L, team.getTeamId()).getMyRole()).isEqualTo(1);

        socialService.unsetTeamAdmin(20001L, team.getTeamId(), 10001L);
        assertThat(socialService.teamDetail(10001L, team.getTeamId()).getMyRole()).isZero();
    }

    @Test
    void teamContentScoresAndVotesWork() {
        mockUsers();
        TeamResponse team = socialService.createTeam(10001L, teamRequest("内容协作小队", 0));
        socialService.joinTeam(20001L, team.getTeamId(), null);

        TeamAnnouncementRequest announcementRequest = new TeamAnnouncementRequest();
        announcementRequest.setTitle("周末集合");
        announcementRequest.setContent("周六上午十点集合。");
        TeamAnnouncementResponse announcement = socialService.publishAnnouncement(10001L, team.getTeamId(), announcementRequest);
        assertThat(announcement.getAnnouncementId()).isNotNull();
        assertThat(socialService.announcements(team.getTeamId())).hasSize(1);
        assertThatThrownBy(() -> socialService.publishAnnouncement(20001L, team.getTeamId(), announcementRequest))
                .isInstanceOf(BizException.class);

        TeamAlbumRequest albumRequest = new TeamAlbumRequest();
        albumRequest.setImageUrl("/uploads/team-album/1.jpg");
        albumRequest.setDescription("活动合影");
        assertThat(socialService.addAlbumImage(20001L, team.getTeamId(), albumRequest).getAlbumId()).isNotNull();
        assertThat(socialService.album(team.getTeamId())).hasSize(1);

        TeamFileRequest fileRequest = new TeamFileRequest();
        fileRequest.setFileName("路线.pdf");
        fileRequest.setFileUrl("/uploads/team-file/route.pdf");
        fileRequest.setFileSize(1024L);
        assertThat(socialService.addTeamFile(20001L, team.getTeamId(), fileRequest).getFileId()).isNotNull();
        assertThat(socialService.files(team.getTeamId())).hasSize(1);

        List<TeamScoreResponse> scores = socialService.scores(team.getTeamId());
        assertThat(scores).extracting(TeamScoreResponse::getUserId).containsExactly(10001L, 20001L);
        assertThat(scores).extracting(TeamScoreResponse::getScore).containsExactly(2, 2);

        TeamVoteCreateRequest voteRequest = new TeamVoteCreateRequest();
        voteRequest.setTitle("午餐选择");
        voteRequest.setDescription("选一个聚餐地点");
        voteRequest.setMultiple(false);
        voteRequest.setDeadline(LocalDateTime.now().plusDays(1));
        voteRequest.setOptions(List.of("面馆", "简餐"));
        TeamVoteResponse vote = socialService.createVote(10001L, team.getTeamId(), voteRequest);
        assertThat(vote.getOptions()).hasSize(2);

        TeamVoteCastRequest castRequest = new TeamVoteCastRequest();
        castRequest.setOptionIds(List.of(vote.getOptions().get(0).getOptionId()));
        TeamVoteResponse voted = socialService.castVote(20001L, team.getTeamId(), vote.getVoteId(), castRequest);
        assertThat(voted.getOptions().get(0).getVoteCount()).isEqualTo(1);
        assertThat(voted.getOptions().get(0).getSelected()).isTrue();
        assertThat(socialService.votes(20001L, team.getTeamId()).get(0).getOptions().get(0).getSelected()).isTrue();
        assertThatThrownBy(() -> socialService.castVote(20001L, team.getTeamId(), vote.getVoteId(), castRequest))
                .isInstanceOf(BizException.class);
    }

    private TeamCreateRequest teamRequest(String name, int joinType) {
        TeamCreateRequest request = new TeamCreateRequest();
        request.setName(name);
        request.setDescription("小队测试");
        request.setTags(List.of("测试", "小队"));
        request.setJoinType(joinType);
        request.setMaxMembers(10);
        return request;
    }

    private ReviewRequest approve() {
        ReviewRequest request = new ReviewRequest();
        request.setAction(1);
        return request;
    }

    private void mockUsers() {
        when(userClient.isUserValid(anyLong())).thenReturn(true);
        when(userClient.getUsersByIds(anyList())).thenAnswer(invocation -> {
            List<Long> ids = invocation.getArgument(0);
            return ids.stream()
                    .distinct()
                    .map(id -> new UserBasicDTO(id, "user-" + id, "/avatar/" + id + ".png", 0))
                    .collect(Collectors.toMap(UserBasicDTO::getUserId, Function.identity(), (left, right) -> left));
        });
    }
}
