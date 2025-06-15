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

import java.io.InputStream;
import java.util.List;

@Component
public class ExtOjLinkDataLoader {

    private static final Logger log = LoggerFactory.getLogger(ExtOjLinkDataLoader.class);
    private final ExtOjLinkRepository repository;
    private final ObjectMapper objectMapper;

    public ExtOjLinkDataLoader(ExtOjLinkRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
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
