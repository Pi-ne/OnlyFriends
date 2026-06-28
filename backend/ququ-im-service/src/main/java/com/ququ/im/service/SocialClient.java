package com.ququ.im.service;

public interface SocialClient {
    boolean areFriends(Long userIdA, Long userIdB);

    boolean isTeamMember(Long teamId, Long userId);

    java.util.List<Long> getTeamMemberIds(Long teamId);
}
