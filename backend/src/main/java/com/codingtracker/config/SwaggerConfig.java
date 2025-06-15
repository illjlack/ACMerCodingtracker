package com.codingtracker.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger API文档配置
 * 
 * @author CodingTracker Team
 * @version 2.0.0
 */
@Configuration
public class SwaggerConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${app.name:CodingTracker}")
    private String appName;

    @Value("${app.version:2.0.0}")
    private String appVersion;

    @Value("${app.description:编程练习跟踪系统}")
    private String appDescription;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title(appName + " API")
                        .version(appVersion)
                        .description(appDescription + " - RESTful API 文档")
                        .contact(new Contact()
                                .name("CodingTracker Team")
                                .url("https://github.com/your-org/coding-tracker")
                                .email("contact@codingtracker.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("开发环境"),
                        new Server()
                                .url("https://api.codingtracker.com")
                                .description("生产环境")))
                .addSecurityItem(new SecurityRequirement().addList("JWT"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("JWT", new SecurityScheme()
                                .name("JWT")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)
                                .description("JWT认证令牌")));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public")
                .displayName("公开API")
                .pathsToMatch("/api/v1/**")
                .packagesToScan("com.codingtracker.api.v1.controller")
                .build();
    }

    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("auth")
                .displayName("认证API")
                .pathsToMatch("/api/v1/auth/**")
                .packagesToScan("com.codingtracker.api.v1.controller")
                .build();
    }

    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("user")
                .displayName("用户API")
                .pathsToMatch("/api/v1/users/**")
                .packagesToScan("com.codingtracker.api.v1.controller")
                .build();
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("admin")
                .displayName("管理API")
                .pathsToMatch("/api/v1/admin/**")
                .packagesToScan("com.codingtracker.api.v1.controller")
                .build();
    }

    @Bean
    public GroupedOpenApi fileApi() {
        return GroupedOpenApi.builder()
                .group("file")
                .displayName("文件API")
                .pathsToMatch("/api/files/**")
                .packagesToScan("com.codingtracker.api.v1.controller")
                .build();
    }
}