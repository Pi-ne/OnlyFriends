package com.onlyfriends.admin.client;

import com.onlyfriends.admin.dto.request.OfflineActivityRequest;
import com.onlyfriends.admin.dto.request.ReviewRequest;
import com.onlyfriends.common.response.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "activity-service", contextId = "adminActivityClient", url = "${activity-service.base-url:http://localhost:8082}")
public interface ActivityAdminClient {
    @GetMapping("/internal/admin/activities/pending")
    Result<Object> pendingActivities(@RequestParam("page") Integer page, @RequestParam("size") Integer size);

    @GetMapping("/internal/admin/activities")
    Result<Object> activities(@RequestParam("page") Integer page,
                              @RequestParam("size") Integer size,
                              @RequestParam(value = "status", required = false) Integer status,
                              @RequestParam(value = "keyword", required = false) String keyword);

    @GetMapping("/internal/admin/activities/{id}")
    Result<Object> activityDetail(@PathVariable("id") Long activityId);

    @PutMapping("/internal/admin/activities/{id}/review")
    Result<Void> review(@PathVariable("id") Long activityId,
                        @RequestBody ReviewRequest request);

    @PostMapping("/internal/admin/activities/{id}/offline")
    Result<Void> offline(@PathVariable("id") Long activityId,
                          @RequestBody OfflineActivityRequest request);

    @PostMapping("/internal/admin/activities/{id}/restore")
    Result<Void> restore(@PathVariable("id") Long activityId);
}
