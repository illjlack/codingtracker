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
import java.util.Map;

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

        Map<String, ExtOjLinkDTO> map = objectMapper.readValue(is, new TypeReference<>() {});

        for (Map.Entry<String, ExtOjLinkDTO> entry : map.entrySet()) {
            ExtOjLinkDTO dto = entry.getValue();

            ExtOjLink entity = ExtOjLink.builder()
                    // 用自定义的方法从jsonKey转成枚举
                    .oj(OJPlatform.fromName(entry.getKey()))
                    .indexLink(dto.indexLink)
                    .userInfoLink(dto.userInfoLink)
                    .pbStatusLink(dto.pbStatusLink)
                    .problemLink(dto.problemLink)
                    .loginLink(dto.loginLink)
                    .authToken(dto.authToken)
                    .build();

            repository.save(entity);
            log.info("saved oj link {}", entity);
        }
    }

    private static class ExtOjLinkDTO {
        public String indexLink;
        public String userInfoLink;
        public String pbStatusLink;
        public String problemLink;
        public String loginLink;
        public String authToken;
    }
}
