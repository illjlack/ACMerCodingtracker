package com.codingtracker.controller.api.auth;

import com.codingtracker.dto.ApiResponse;
import com.codingtracker.model.User;
import com.codingtracker.model.UserOJ;
import com.codingtracker.service.UserService;
import com.codingtracker.service.EmailCodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/email")
public class EmailCodeController {

    private static final Logger logger = LoggerFactory.getLogger(EmailCodeController.class);

    @Autowired
    private EmailCodeService emailCodeService;

    @Autowired
    private UserService userService;

    @PostMapping("/sendCode")
    public ApiResponse<String> sendCode(@RequestParam String email) {
        emailCodeService.sendCode(email);
        return ApiResponse.ok("验证码已发送，请注意查收");
    }

    @PostMapping("/verifyCode")
    public ApiResponse<Boolean> verifyCode(@RequestParam String email, @RequestParam String code) {
        boolean valid = emailCodeService.verifyCode(email, code);
        if (valid) {
            return ApiResponse.ok();
        } else {
            return ApiResponse.error("验证码错误或已过期");
        }
    }

    // 修改密码接口
    @PutMapping("/modifyPassword")
    public ApiResponse<Void> changePassword(
            @RequestParam String username,
            @RequestParam String newPassword,
            @RequestParam String email,
            @RequestParam String code) {

        logger.info("用户尝试修改密码: 用户名={}, 请求邮箱={}", username, email);

        // 1. 查询用户
        Optional<User> user = userService.findByUsername(username);
        if (user.isEmpty()) {
            logger.warn("修改密码失败，用户不存在: 用户名={}", username);
            return ApiResponse.error("用户不存在");
        }

        // 2. 验证请求邮箱是否与账户邮箱匹配
        String registeredEmail = user.get().getEmail();
        if (registeredEmail == null || !registeredEmail.equalsIgnoreCase(email)) {
            logger.warn("修改密码失败，邮箱与账号不匹配: 用户名={}, 请求邮箱={}, 绑定邮箱={}",
                    username, email, registeredEmail);
            return ApiResponse.error("邮箱与账号不匹配");
        }

        // 3. 验证验证码是否有效
        boolean validCode = emailCodeService.verifyCode(email, code);
        if (!validCode) {
            logger.warn("验证码验证失败: 用户名={}, 邮箱={}", username, email);
            return ApiResponse.error("验证码错误或已过期");
        }

        // 4. 修改密码
        userService.modifyUserPassword(user.get().getId(), newPassword);
        logger.info("用户密码修改成功: 用户名={}", username);
        return ApiResponse.ok("密码修改成功", null);
    }
}
