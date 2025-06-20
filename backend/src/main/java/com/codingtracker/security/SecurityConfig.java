package com.codingtracker.security;

import com.codingtracker.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 这个 Bean 用来给 cors() 用的
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        // 允许所有域名发起跨域
        cfg.setAllowedOriginPatterns(List.of("*"));
        // 允许这些方法
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // 允许带这些头
        cfg.setAllowedHeaders(List.of("*"));
        // 允许带 Cookie / Authorization
        cfg.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        // 对所有 /api/** 路径都应用上面这个 CORS 配置
        src.registerCorsConfiguration("/api/**", cfg);
        return src;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 先开启 cors
                .cors(Customizer.withDefaults())
                // 然后再禁用 csrf
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // 允许所有预检请求
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 登录 & 注册 不需要认证
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/email/sendCode").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/email/modifyPassword").permitAll()
                        // 允许访问头像资源
                        .requestMatchers("/avatars/**").permitAll()
                        // 其它接口都需要走 JWT 认证
                        .anyRequest().authenticated())
                // 在 UsernamePasswordFilter 之前插入你的 JWT 校验过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // 不启用默认的 basic auth
                .httpBasic(AbstractHttpConfigurer::disable)
                // 配置异常处理
                .exceptionHandling(exceptions -> exceptions
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"success\":false,\"message\":\"权限不足，无法访问该资源\",\"code\":403}");
                        })
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"success\":false,\"message\":\"未认证，请先登录\",\"code\":401}");
                        }));

        return http.build();
    }

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
}
