package com.onlyfriends.admin.client;

import com.onlyfriends.admin.dto.request.BanUserRequest;
import com.onlyfriends.admin.dto.request.ReviewRequest;
import com.onlyfriends.common.response.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "user-service", contextId = "adminUserClient", url = "${user-service.base-url:http://localhost:8081}")
public interface UserAdminClient {
    @GetMapping("/internal/admin/users")
    Result<Object> users(@RequestParam("page") Integer page,
                         @RequestParam("size") Integer size,
                         @RequestParam(value = "userType", required = false) Integer userType,
                         @RequestParam(value = "status", required = false) Integer status,
                         @RequestParam(value = "email", required = false) String email,
                         @RequestParam(value = "nickname", required = false) String nickname,
                         @RequestParam(value = "keyword", required = false) String keyword);

    @GetMapping("/internal/admin/users/{id}")
    Result<Object> userDetail(@PathVariable("id") Long userId);

    @PostMapping("/internal/admin/users/{id}/ban")
    Result<Void> banUser(@RequestParam("adminId") Long adminId,
                         @PathVariable("id") Long userId,
                         @RequestBody BanUserRequest request);

    @PostMapping("/internal/admin/users/{id}/unban")
    Result<Void> unbanUser(@PathVariable("id") Long userId);

    @GetMapping("/internal/admin/merchant-applies")
    Result<Object> merchantApplies(@RequestParam("page") Integer page,
                                   @RequestParam("size") Integer size,
                                   @RequestParam(value = "status", required = false) Integer status);

    @GetMapping("/internal/admin/merchant-applies/{id}")
    Result<Object> merchantApplyDetail(@PathVariable("id") Long applyId);

    @PutMapping("/internal/admin/merchant-applies/{id}/review")
    Result<Void> reviewMerchantApply(@RequestParam("adminId") Long adminId,
                                     @PathVariable("id") Long applyId,
                                     @RequestBody ReviewRequest request);
}
