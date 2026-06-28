package com.ququ.user.service;

public interface MailService {
    void sendActivationMail(String email, String nickname, String activateToken);
}
