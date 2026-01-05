package dev.dong4j.zeka.stack.idea.plugin.common.autocomplete;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class NextEditResponseParser {
    private static final Pattern CODE_FENCE = Pattern.compile("```(?:[a-zA-Z0-9]+)?\\n([\\s\\S]*?)\\n```", Pattern.MULTILINE);
    private final ObjectMapper mapper = new ObjectMapper();

    @NotNull
    List<NextEditAutocompletion> parse(@NotNull String raw) {
        String payload = stripCodeFence(raw.trim());
        if (payload.isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = mapper.readTree(payload);
            if (root == null) {
                return List.of();
            }
            JsonNode listNode = root.isArray() ? root : root.get("edits");
            if (listNode == null || !listNode.isArray()) {
                return List.of();
            }
            List<NextEditAutocompletion> results = new ArrayList<>();
            for (JsonNode node : listNode) {
                int start = node.path("start_index").asInt(-1);
                int end = node.path("end_index").asInt(-1);
                String completion = node.path("completion").asText("");
                float confidence = (float) node.path("confidence").asDouble(0.0);
                String id = node.path("autocomplete_id").asText(UUID.randomUUID().toString());
                if (start >= 0 && end >= 0) {
                    results.add(new NextEditAutocompletion(start, end, completion, confidence, id));
                }
            }
            return results;
        } catch (Exception e) {
            return List.of();
        }
    }

    @NotNull
    private String stripCodeFence(@NotNull String raw) {
        Matcher matcher = CODE_FENCE.matcher(raw);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return raw;
    }
}
