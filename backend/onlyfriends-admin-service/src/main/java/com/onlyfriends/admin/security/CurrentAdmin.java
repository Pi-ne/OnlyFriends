package com.onlyfriends.admin.security;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CurrentAdmin {
    private Long adminId;
    private String username;
    private String nickname;
}
