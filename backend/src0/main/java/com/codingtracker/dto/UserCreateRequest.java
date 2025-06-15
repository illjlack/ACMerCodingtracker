package com.codingtracker.dto;

import com.codingtracker.model.OJPlatform;
import com.codingtracker.model.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Set;

/**
 * 用户创建请求DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class UserCreateRequest {

    private String username;
    private String password;
    private String realName;
    private String email;
    private String major;
    private Set<User.Type> roles;
    private List<Long> tags;
    private List<OJAccountRequest> ojAccounts;

    /**
     * OJ账号请求DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class OJAccountRequest {
        private OJPlatform platform;
        private String accountName;
    }
}