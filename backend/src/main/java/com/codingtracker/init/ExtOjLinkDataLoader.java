package com.codingtracker.init;

import com.codingtracker.model.ExtOjLink;
import com.codingtracker.model.OJPlatform;
import com.codingtracker.repository.ExtOjLinkRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.File;
import java.io.InputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Component
public class ExtOjLinkDataLoader {

    private static final Logger log = LoggerFactory.getLogger(ExtOjLinkDataLoader.class);
    private static final String OJ_LINKS_FILE = "src/main/resources/oj_links.json";

    private final ExtOjLinkRepository repository;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    public ExtOjLinkDataLoader(ExtOjLinkRepository repository, ObjectMapper objectMapper,
            ResourceLoader resourceLoader) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void init() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("oj_links.json");

        List<ExtOjLinkDTO> dtoList = objectMapper.readValue(is, new TypeReference<>() {
        });

        for (ExtOjLinkDTO dto : dtoList) {
            ExtOjLink entity = ExtOjLink.builder()
                    .oj(OJPlatform.fromName(dto.oj.toLowerCase()))
                    .homepageLink(dto.homepageLink)
                    .loginPageLink(dto.loginPageLink)
                    .submissionRecordsLink(dto.submissionRecordsLink)
                    .userInfoLink(dto.userInfoLink)
                    .pbStatusLink(dto.pbStatusLink)
                    .problemLink(dto.problemLink)
                    .loginLink(dto.loginLink)
                    .authToken(dto.authToken)
                    .tokenFormat(dto.tokenFormat)
                    .requiresToken(dto.requiresToken)
                    .build();

            repository.save(entity);
            log.info("saved oj link {}", entity);
        }
    }

    /**
     * 将数据库中的OJ链接配置保存到JSON文件
     */
    public void saveToFile() {
        try {
            List<ExtOjLink> allLinks = repository.findAll();
            List<ExtOjLinkDTO> dtoList = allLinks.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            // 获取项目根目录并构建文件路径
            File file = new File(OJ_LINKS_FILE);
            if (!file.exists()) {
                // 如果文件不存在，尝试在当前工作目录下创建
                file = new File("oj_links.json");
            }

            // 保存到文件
            try (FileWriter writer = new FileWriter(file)) {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(writer, dtoList);
                log.info("成功将OJ链接配置保存到文件: {}", file.getAbsolutePath());
            }
        } catch (IOException e) {
            log.error("保存OJ链接配置到文件失败: {}", e.getMessage(), e);
            throw new RuntimeException("保存配置文件失败", e);
        } catch (Exception e) {
            log.error("保存OJ链接配置时发生未知错误: {}", e.getMessage(), e);
            throw new RuntimeException("保存配置失败", e);
        }
    }

    /**
     * 转换ExtOjLink实体为DTO
     */
    private ExtOjLinkDTO convertToDTO(ExtOjLink entity) {
        ExtOjLinkDTO dto = new ExtOjLinkDTO();
        dto.oj = entity.getOj().name();
        dto.homepageLink = entity.getHomepageLink();
        dto.loginPageLink = entity.getLoginPageLink();
        dto.submissionRecordsLink = entity.getSubmissionRecordsLink();
        dto.userInfoLink = entity.getUserInfoLink();
        dto.pbStatusLink = entity.getPbStatusLink();
        dto.problemLink = entity.getProblemLink();
        dto.loginLink = entity.getLoginLink();
        dto.authToken = entity.getAuthToken();
        dto.tokenFormat = entity.getTokenFormat();
        dto.requiresToken = entity.getRequiresToken();
        return dto;
    }

    private static class ExtOjLinkDTO {
        public String oj;
        public String homepageLink;
        public String loginPageLink;
        public String submissionRecordsLink;
        public String userInfoLink;
        public String pbStatusLink;
        public String problemLink;
        public String loginLink;
        public String authToken;
        public String tokenFormat;
        public Boolean requiresToken;
    }
}
