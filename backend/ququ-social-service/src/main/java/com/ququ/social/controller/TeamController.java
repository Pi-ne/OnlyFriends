package com.ququ.social.controller;

import com.ququ.common.exception.BizException;
import com.ququ.common.response.Result;
import com.ququ.common.response.ResultCode;
import com.ququ.common.storage.FileStorageService;
import com.ququ.social.dto.request.ReviewRequest;
import com.ququ.social.dto.request.TeamAlbumRequest;
import com.ququ.social.dto.request.TeamAnnouncementRequest;
import com.ququ.social.dto.request.TeamCreateRequest;
import com.ququ.social.dto.request.TeamFileRequest;
import com.ququ.social.dto.request.TeamJoinRequest;
import com.ququ.social.dto.request.TeamVoteCastRequest;
import com.ququ.social.dto.request.TeamVoteCreateRequest;
import com.ququ.social.dto.response.TeamAlbumResponse;
import com.ququ.social.dto.response.TeamAnnouncementResponse;
import com.ququ.social.dto.response.TeamFileResponse;
import com.ququ.social.dto.response.TeamJoinApplyResponse;
import com.ququ.social.dto.response.TeamMemberResponse;
import com.ququ.social.dto.response.TeamResponse;
import com.ququ.social.dto.response.TeamScoreResponse;
import com.ququ.social.dto.response.TeamVoteResponse;
import com.ququ.social.security.CurrentUser;
import com.ququ.social.service.SocialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/teams")
public class TeamController {
    private final SocialService socialService;
    private final FileStorageService fileStorageService;

    @PostMapping
    public Result<TeamResponse> create(@AuthenticationPrincipal CurrentUser currentUser,
                                       @Valid @RequestBody TeamCreateRequest request) {
        return Result.success(socialService.createTeam(requireUser(currentUser).getUserId(), request));
    }

    @GetMapping
    public Result<List<TeamResponse>> teams(@AuthenticationPrincipal CurrentUser currentUser,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Long ownerId,
                                            @RequestParam(required = false) Boolean joined) {
        Long joinedUserId = Boolean.TRUE.equals(joined) ? requireUser(currentUser).getUserId() : null;
        return Result.success(socialService.teams(keyword, ownerId, joinedUserId));
    }

    @GetMapping("/{id}")
    public Result<TeamResponse> detail(@AuthenticationPrincipal CurrentUser currentUser,
                                       @PathVariable Long id) {
        Long currentUserId = currentUser == null ? null : currentUser.getUserId();
        return Result.success(socialService.teamDetail(currentUserId, id));
    }

    @PostMapping("/{id}/join")
    public Result<Map<String, Long>> join(@AuthenticationPrincipal CurrentUser currentUser,
                                          @PathVariable Long id,
                                          @Valid @RequestBody(required = false) TeamJoinRequest request) {
        Long applyId = socialService.joinTeam(requireUser(currentUser).getUserId(), id, request);
        return Result.success(Map.of("applyId", applyId == null ? 0L : applyId));
    }

    @GetMapping("/{id}/join-applies")
    public Result<List<TeamJoinApplyResponse>> joinApplies(@AuthenticationPrincipal CurrentUser currentUser,
                                                           @PathVariable Long id) {
        return Result.success(socialService.teamJoinApplies(requireUser(currentUser).getUserId(), id));
    }

    @PutMapping("/{id}/join-applies/{applyId}")
    public Result<Void> reviewJoinApply(@AuthenticationPrincipal CurrentUser currentUser,
                                        @PathVariable Long id,
                                        @PathVariable Long applyId,
                                        @Valid @RequestBody ReviewRequest request) {
        socialService.reviewTeamJoinApply(requireUser(currentUser).getUserId(), id, applyId, request);
        return Result.success();
    }

    @GetMapping("/{id}/members")
    public Result<List<TeamMemberResponse>> members(@PathVariable Long id) {
        return Result.success(socialService.teamMembers(id));
    }

    @DeleteMapping("/{id}/members/me")
    public Result<Void> leave(@AuthenticationPrincipal CurrentUser currentUser,
                              @PathVariable Long id) {
        socialService.leaveTeam(requireUser(currentUser).getUserId(), id);
        return Result.success();
    }

    @DeleteMapping("/{id}/members/{userId}")
    public Result<Void> removeMember(@AuthenticationPrincipal CurrentUser currentUser,
                                     @PathVariable Long id,
                                     @PathVariable Long userId) {
        socialService.removeTeamMember(requireUser(currentUser).getUserId(), id, userId);
        return Result.success();
    }

    @PutMapping("/{id}/members/{userId}/admin")
    public Result<Void> setAdmin(@AuthenticationPrincipal CurrentUser currentUser,
                                 @PathVariable Long id,
                                 @PathVariable Long userId) {
        socialService.setTeamAdmin(requireUser(currentUser).getUserId(), id, userId);
        return Result.success();
    }

    @DeleteMapping("/{id}/members/{userId}/admin")
    public Result<Void> unsetAdmin(@AuthenticationPrincipal CurrentUser currentUser,
                                   @PathVariable Long id,
                                   @PathVariable Long userId) {
        socialService.unsetTeamAdmin(requireUser(currentUser).getUserId(), id, userId);
        return Result.success();
    }

    @PutMapping("/{id}/owner/{userId}")
    public Result<Void> transferOwner(@AuthenticationPrincipal CurrentUser currentUser,
                                      @PathVariable Long id,
                                      @PathVariable Long userId) {
        socialService.transferTeamOwner(requireUser(currentUser).getUserId(), id, userId);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> dissolve(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable Long id) {
        socialService.dissolveTeam(requireUser(currentUser).getUserId(), id);
        return Result.success();
    }

    @PostMapping("/{id}/announcements")
    public Result<TeamAnnouncementResponse> publishAnnouncement(@AuthenticationPrincipal CurrentUser currentUser,
                                                                @PathVariable Long id,
                                                                @Valid @RequestBody TeamAnnouncementRequest request) {
        return Result.success(socialService.publishAnnouncement(requireUser(currentUser).getUserId(), id, request));
    }

    @GetMapping("/{id}/announcements")
    public Result<List<TeamAnnouncementResponse>> announcements(@PathVariable Long id) {
        return Result.success(socialService.announcements(id));
    }

    @PostMapping("/{id}/album")
    public Result<TeamAlbumResponse> addAlbumImage(@AuthenticationPrincipal CurrentUser currentUser,
                                                   @PathVariable Long id,
                                                   @Valid @RequestBody TeamAlbumRequest request) {
        return Result.success(socialService.addAlbumImage(requireUser(currentUser).getUserId(), id, request));
    }

    @PostMapping("/{id}/album/upload")
    public Result<TeamAlbumResponse> uploadAlbumImage(@AuthenticationPrincipal CurrentUser currentUser,
                                                      @PathVariable Long id,
                                                      @RequestParam("file") MultipartFile file,
                                                      @RequestParam(required = false) String description) {
        TeamAlbumRequest request = new TeamAlbumRequest();
        request.setImageUrl(fileStorageService.upload("team-album", file));
        request.setDescription(description);
        return Result.success(socialService.addAlbumImage(requireUser(currentUser).getUserId(), id, request));
    }

    @GetMapping("/{id}/album")
    public Result<List<TeamAlbumResponse>> album(@PathVariable Long id) {
        return Result.success(socialService.album(id));
    }

    @PostMapping("/{id}/files")
    public Result<TeamFileResponse> addFile(@AuthenticationPrincipal CurrentUser currentUser,
                                            @PathVariable Long id,
                                            @Valid @RequestBody TeamFileRequest request) {
        return Result.success(socialService.addTeamFile(requireUser(currentUser).getUserId(), id, request));
    }

    @PostMapping("/{id}/files/upload")
    public Result<TeamFileResponse> uploadFile(@AuthenticationPrincipal CurrentUser currentUser,
                                               @PathVariable Long id,
                                               @RequestParam("file") MultipartFile file) {
        TeamFileRequest request = new TeamFileRequest();
        request.setFileName(file.getOriginalFilename());
        request.setFileSize(file.getSize());
        request.setFileUrl(fileStorageService.upload("team-file", file));
        return Result.success(socialService.addTeamFile(requireUser(currentUser).getUserId(), id, request));
    }

    @GetMapping("/{id}/files")
    public Result<List<TeamFileResponse>> files(@PathVariable Long id) {
        return Result.success(socialService.files(id));
    }

    @GetMapping("/{id}/scores")
    public Result<List<TeamScoreResponse>> scores(@PathVariable Long id) {
        return Result.success(socialService.scores(id));
    }

    @PostMapping("/{id}/votes")
    public Result<TeamVoteResponse> createVote(@AuthenticationPrincipal CurrentUser currentUser,
                                               @PathVariable Long id,
                                               @Valid @RequestBody TeamVoteCreateRequest request) {
        return Result.success(socialService.createVote(requireUser(currentUser).getUserId(), id, request));
    }

    @GetMapping("/{id}/votes")
    public Result<List<TeamVoteResponse>> votes(@AuthenticationPrincipal CurrentUser currentUser,
                                                @PathVariable Long id) {
        Long currentUserId = currentUser == null ? null : currentUser.getUserId();
        return Result.success(socialService.votes(currentUserId, id));
    }

    @PostMapping("/{id}/votes/{voteId}/records")
    public Result<TeamVoteResponse> castVote(@AuthenticationPrincipal CurrentUser currentUser,
                                             @PathVariable Long id,
                                             @PathVariable Long voteId,
                                             @Valid @RequestBody TeamVoteCastRequest request) {
        return Result.success(socialService.castVote(requireUser(currentUser).getUserId(), id, voteId, request));
    }

    private CurrentUser requireUser(CurrentUser currentUser) {
        if (currentUser == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return currentUser;
    }
}
