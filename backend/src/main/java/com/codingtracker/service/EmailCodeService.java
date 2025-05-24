package com.codingtracker.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EmailCodeService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    // 简单内存缓存，key为邮箱，value为验证码和过期时间
    private final Map<String, CodeData> codeCache = new ConcurrentHashMap<>();

    private static final long EXPIRE_MINUTES = 5;  // 5分钟有效

    private static class CodeData {
        String code;
        LocalDateTime expireTime;

        CodeData(String code, LocalDateTime expireTime) {
            this.code = code;
            this.expireTime = expireTime;
        }
    }

    // 生成6位数字验证码
    private String generateCode() {
        Random random = new Random();
        int num = 100000 + random.nextInt(900000);
        return String.valueOf(num);
    }

    public void sendCode(String toEmail) {
        String code = generateCode();
        LocalDateTime expire = LocalDateTime.now().plusMinutes(EXPIRE_MINUTES);

        // 缓存验证码
        codeCache.put(toEmail, new CodeData(code, expire));

        // 发送邮件
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("验证码通知");
        message.setText("您的验证码是：" + code + "，有效期5分钟，请尽快使用。");
        message.setFrom(fromEmail);

        mailSender.send(message);
    }

    // 验证验证码是否正确且未过期
    public boolean verifyCode(String email, String inputCode) {
        CodeData codeData = codeCache.get(email);
        if (codeData == null) return false;
        if (LocalDateTime.now().isAfter(codeData.expireTime)) {
            codeCache.remove(email);  // 过期移除
            return false;
        }
        boolean match = codeData.code.equals(inputCode);
        if (match) {
            codeCache.remove(email);  // 验证成功后移除
        }
        return match;
    }
}
