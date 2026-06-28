package com.ququ.user.controller.internal;

import com.ququ.common.dto.UserBasicDTO;
import com.ququ.common.response.Result;
import com.ququ.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/users")
public class UserInternalController {
    private final UserService userService;

    @GetMapping("/batch")
    public Result<List<UserBasicDTO>> getUsersByIds(@RequestParam List<Long> ids) {
        return Result.success(userService.getUsersByIds(ids));
    }

    @GetMapping("/{id}/valid")
    public Result<Boolean> isUserValid(@PathVariable Long id) {
        return Result.success(userService.isUserValid(id));
    }

    @GetMapping("/{id}/credit")
    public Result<Integer> getUserCredit(@PathVariable Long id) {
        return Result.success(userService.getUserCredit(id));
    }

    @PostMapping("/{id}/credit/deduct")
    public Result<Void> deductCredit(@PathVariable Long id, @RequestParam Integer amount) {
        userService.deductCredit(id, amount);
        return Result.success();
    }
}
