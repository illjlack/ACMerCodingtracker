package com.codingtracker.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT认证入口点
 * 处理未认证访问的情况
 * 
 * @author CodingTracker Team
 * @version 2.0.0
 */
@Component
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {

        log.warn("Unauthorized access attempt: {} - {}", request.getRequestURI(), authException.getMessage());

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> errorResponse = createErrorResponse(request, authException);

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    /**
     * 创建错误响应
     */
    private Map<String, Object> createErrorResponse(HttpServletRequest request,
            AuthenticationException authException) {
        Map<String, Object> errorResponse = new HashMap<>();

        errorResponse.put("success", false);
        errorResponse.put("code", 401);
        errorResponse.put("message", "认证失败，请先登录");
        errorResponse.put("data", null);
        errorResponse.put("timestamp", LocalDateTime.now().toString());
        errorResponse.put("path", request.getRequestURI());

        // 根据异常类型提供更具体的错误信息
        String detailMessage = getDetailMessage(authException);
        if (detailMessage != null) {
            errorResponse.put("detail", detailMessage);
        }

        return errorResponse;
    }

    /**
     * 根据异常类型获取详细错误信息
     */
    private String getDetailMessage(AuthenticationException authException) {
        String exceptionName = authException.getClass().getSimpleName();

        switch (exceptionName) {
            case "BadCredentialsException":
                return "用户名或密码错误";
            case "AccountExpiredException":
                return "账户已过期";
            case "CredentialsExpiredException":
                return "凭证已过期";
            case "DisabledException":
                return "账户已被禁用";
            case "LockedException":
                return "账户已被锁定";
            case "InsufficientAuthenticationException":
                return "认证信息不足";
            default:
                return "认证失败";
        }
    }
}