package com.ququ.activity.client;

import com.ququ.common.dto.UserBasicDTO;
import com.ququ.common.response.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "user-service", contextId = "activityUserFeignClient", url = "${user-service.base-url:http://localhost:8081}")
public interface UserFeignClient {
    @GetMapping("/internal/users/{id}/valid")
    Result<Boolean> isUserValid(@PathVariable("id") Long id);

    @GetMapping("/internal/users/{id}/credit")
    Result<Integer> getUserCredit(@PathVariable("id") Long id);

    @GetMapping("/internal/users/batch")
    Result<List<UserBasicDTO>> getUsersByIds(@RequestParam("ids") List<Long> ids);
}
