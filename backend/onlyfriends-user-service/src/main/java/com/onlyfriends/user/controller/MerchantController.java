package com.onlyfriends.user.controller;

import com.onlyfriends.common.response.Result;
import com.onlyfriends.common.storage.FileStorageService;
import com.onlyfriends.user.dto.request.MerchantApplyRequest;
import com.onlyfriends.user.dto.response.MerchantInfoResponse;
import com.onlyfriends.user.dto.response.MerchantApplyStatusResponse;
import com.onlyfriends.user.security.CurrentUser;
import com.onlyfriends.user.service.MerchantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/merchant")
public class MerchantController {
    private final MerchantService merchantService;
    private final FileStorageService fileStorageService;

    @PostMapping("/apply")
    public Result<Map<String, Long>> apply(@AuthenticationPrincipal CurrentUser currentUser,
                                           @Valid @RequestBody MerchantApplyRequest request) {
        Long applyId = merchantService.apply(currentUser.getUserId(), request);
        return Result.success("商家申请已提交", Map.of("applyId", applyId));
    }

    @GetMapping("/apply/status")
    public Result<MerchantApplyStatusResponse> status(@AuthenticationPrincipal CurrentUser currentUser) {
        return Result.success(merchantService.getApplyStatus(currentUser.getUserId()));
    }

    @GetMapping("/me")
    public Result<MerchantInfoResponse> getMerchantInfo(@AuthenticationPrincipal CurrentUser currentUser) {
        return Result.success(merchantService.getMerchantInfo(currentUser.getUserId()));
    }

    @PostMapping("/license")
    public Result<Map<String, String>> uploadLicense(@AuthenticationPrincipal CurrentUser currentUser,
                                                     @RequestParam("file") MultipartFile file) {
        String licenseUrl = fileStorageService.upload("merchant-license", file);
        return Result.success(Map.of("licenseUrl", licenseUrl));
    }
}
