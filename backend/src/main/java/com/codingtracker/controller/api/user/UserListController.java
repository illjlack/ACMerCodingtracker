package com.codingtracker.controller.api.user;

import com.codingtracker.dto.ApiResponse;
import com.codingtracker.model.User;
import com.codingtracker.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserListController {

    private static final Logger logger = LoggerFactory.getLogger(UserListController.class);

    @Autowired
    private UserService userService;

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        logger.info("请求获取所有用户列表");
        List<User> users = userService.allUser();
        logger.info("成功返回用户列表，共{}个用户", users.size());
        return ResponseEntity.ok(ApiResponse.ok(users));
    }
}