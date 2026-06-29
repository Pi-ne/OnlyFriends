package com.onlyfriends.user.service.impl;

import com.onlyfriends.user.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.activation-base-url:http://localhost:8081/api/v1/auth/activate}")
    private String activationBaseUrl;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Override
    public void sendActivationMail(String email, String nickname, String activateToken) {
        String link = activationBaseUrl + "?token=" + activateToken;
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (!StringUtils.hasText(mailHost) || mailSender == null) {
            log.info("SMTP is not configured. Activation link for {}({}): {}", nickname, email, link);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailUsername);
            message.setTo(email);
            message.setSubject("OnlyFriends 账号激活");
            message.setText("你好，" + nickname + "：\n请点击以下链接激活账号：\n" + link);
            mailSender.send(message);
        } catch (MailException ex) {
            log.warn("Failed to send activation mail to {}. Activation link: {}", email, link, ex);
        }
    }
}
