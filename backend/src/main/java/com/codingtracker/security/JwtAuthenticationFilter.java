package com.codingtracker.security;

import com.codingtracker.service.TokenBlacklistCache;
import com.codingtracker.util.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenBlacklistCache tokenBlacklistCache;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 构造器注入
    public JwtAuthenticationFilter(TokenBlacklistCache tokenBlacklistCache) {
        this.tokenBlacklistCache = tokenBlacklistCache;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String token = getTokenFromRequest(request);

        if (token != null) {
            // 先判断是否在黑名单
            if (tokenBlacklistCache.isTokenBlacklisted(token)) {
                // 返回401和JSON格式的错误信息
                sendUnauthorizedResponse(response, "Token已失效，请重新登录");
                return;
            }

            try {
                Jws<Claims> claimsJws = Jwts.parserBuilder()
                        .setSigningKey(JwtUtils.getSecretKey())
                        .build()
                        .parseClaimsJws(token);

                String username = claimsJws.getBody().getSubject();

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(username,
                        null, null);
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (JwtException e) {
                logger.error("Invalid JWT token", e);
                // 返回401和JSON格式的错误信息
                sendUnauthorizedResponse(response, "Token无效，请重新登录");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", message);
        errorResponse.put("code", 401);

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
