package com.codingtracker.init;

import com.codingtracker.dto.TagMetaDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component
public class TagMetaLoader {
    private final Map<Integer, TagMetaDTO> tagById;

    public TagMetaLoader() {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("luogu-tags.json")) {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(in);
            JsonNode tagsNode = root.path("tags");

            this.tagById = StreamSupport.stream(tagsNode.spliterator(), false)
                    // 把每个 JsonNode 转为 TagMetaDTO
                    .map(node -> mapper.convertValue(node, TagMetaDTO.class))
                    // 用 DTO 的 id 作为 key，DTO 本身作为 value
                    .collect(Collectors.toMap(
                            TagMetaDTO::getId,
                            dto -> dto
                    ));

        } catch (Exception e) {
            throw new RuntimeException("加载 luogu‐tags.json 失败", e);
        }
    }

    /**
     * 根据标签 ID 获取对应的 TagMetaDTO，
     * 如果不存在返回 null。
     */
    public TagMetaDTO get(int id) {
        return tagById.get(id);
    }
}
