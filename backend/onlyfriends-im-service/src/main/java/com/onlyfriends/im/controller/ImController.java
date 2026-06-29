package com.onlyfriends.im.controller;

import com.onlyfriends.common.dto.PageResult;
import com.onlyfriends.common.exception.BizException;
import com.onlyfriends.common.response.Result;
import com.onlyfriends.common.response.ResultCode;
import com.onlyfriends.im.dto.request.GroupMessageRequest;
import com.onlyfriends.im.dto.request.PrivateMessageRequest;
import com.onlyfriends.im.dto.request.ReadRequest;
import com.onlyfriends.im.dto.response.ConversationResponse;
import com.onlyfriends.im.dto.response.MessageResponse;
import com.onlyfriends.im.security.CurrentUser;
import com.onlyfriends.im.service.ImService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/im")
public class ImController {
    private final ImService imService;

    @PostMapping("/messages/private")
    public Result<MessageResponse> sendPrivate(@AuthenticationPrincipal CurrentUser currentUser,
                                               @Valid @RequestBody PrivateMessageRequest request) {
        return Result.success(imService.sendPrivate(requireUser(currentUser).getUserId(), request));
    }

    @PostMapping("/messages/group")
    public Result<MessageResponse> sendGroup(@AuthenticationPrincipal CurrentUser currentUser,
                                             @Valid @RequestBody GroupMessageRequest request) {
        return Result.success(imService.sendGroup(requireUser(currentUser).getUserId(), request));
    }

    @GetMapping("/conversations")
    public Result<List<ConversationResponse>> conversations(@AuthenticationPrincipal CurrentUser currentUser) {
        return Result.success(imService.conversations(requireUser(currentUser).getUserId()));
    }

    @GetMapping("/messages/{convId}")
    public Result<PageResult<MessageResponse>> messages(@AuthenticationPrincipal CurrentUser currentUser,
                                                        @PathVariable Long convId,
                                                        @RequestParam(defaultValue = "1") Integer page,
                                                        @RequestParam(defaultValue = "30") Integer size) {
        return Result.success(imService.messages(requireUser(currentUser).getUserId(), convId, page, size));
    }

    @GetMapping("/groups/{teamId}/messages")
    public Result<PageResult<MessageResponse>> groupMessages(@AuthenticationPrincipal CurrentUser currentUser,
                                                             @PathVariable Long teamId,
                                                             @RequestParam(defaultValue = "1") Integer page,
                                                             @RequestParam(defaultValue = "30") Integer size) {
        return Result.success(imService.groupMessages(requireUser(currentUser).getUserId(), teamId, page, size));
    }

    @PostMapping("/messages/{msgId}/recall")
    public Result<Void> recall(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable Long msgId) {
        imService.recall(requireUser(currentUser).getUserId(), msgId);
        return Result.success();
    }

    @PostMapping("/conversations/{convId}/read")
    public Result<Void> markRead(@AuthenticationPrincipal CurrentUser currentUser,
                                 @PathVariable Long convId,
                                 @Valid @RequestBody ReadRequest request) {
        imService.markRead(requireUser(currentUser).getUserId(), convId, request.getLastReadMsgId());
        return Result.success();
    }

    private CurrentUser requireUser(CurrentUser currentUser) {
        if (currentUser == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return currentUser;
    }
}
