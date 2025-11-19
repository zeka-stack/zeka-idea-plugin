package dev.dong4j.zeka.stack.idea.plugin.nacos.util;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * YamlUtils 单元测试
 *
 * @author dong4j
 * @since 1.0.0
 */
class YamlUtilsTest {

    private static final String SAMPLE_YAML = """
        spring:
          application:
            name: test-app
        server:
          port: 8080
        fkh:
          app:
            config-group: test-group
        """;

    private static final String INVALID_YAML = """
        spring:
          application:
            name: test-app
          invalid: [unclosed array
        """;

    @Test
    void testParseYaml() {
        Map<String, Object> result = YamlUtils.parseYaml(SAMPLE_YAML);
        assertNotNull(result);
        assertFalse(result.isEmpty());

        // 检查解析结果
        Object springObj = result.get("spring");
        assertInstanceOf(Map.class, springObj);

        @SuppressWarnings("unchecked")
        Map<String, Object> springMap = (Map<String, Object>) springObj;
        Object applicationObj = springMap.get("application");
        assertInstanceOf(Map.class, applicationObj);

        @SuppressWarnings("unchecked")
        Map<String, Object> applicationMap = (Map<String, Object>) applicationObj;
        assertEquals("test-app", applicationMap.get("name"));
    }

    @Test
    void testParseInvalidYaml() {
        Map<String, Object> result = YamlUtils.parseYaml(INVALID_YAML);
        assertNull(result);
    }

    @Test
    void testToYaml() {
        new java.util.HashMap<String, Object>() {{
            put("key1", "value1");
            put("key2", 123);
            put("nested", new java.util.HashMap<String, Object>() {{
                put("subkey", "subvalue");
            }});
        }};

        Map<String, Object> data = new java.util.HashMap<>();
        data.put("key1", "value1");
        data.put("key2", 123);
        Map<String, Object> nested = new java.util.HashMap<>();
        nested.put("subkey", "subvalue");
        data.put("nested", nested);

        String yaml = YamlUtils.toYaml(data);
        assertNotNull(yaml);
        assertFalse(yaml.isEmpty());

        // 验证可以重新解析
        Map<String, Object> parsed = YamlUtils.parseYaml(yaml);
        assertNotNull(parsed);
        assertEquals("value1", parsed.get("key1"));
    }

    @Test
    void testExtractAppName() {
        String appName = YamlUtils.extractAppName(SAMPLE_YAML);
        assertEquals("test-app", appName);
    }

    @Test
    void testExtractAppNameNotFound() {
        String yaml = "key: value\n";
        String appName = YamlUtils.extractAppName(yaml);
        assertNull(appName);
    }

    @Test
    void testExtractConfigGroup() {
        String configGroup = YamlUtils.extractConfigGroup(SAMPLE_YAML);
        assertEquals("test-group", configGroup);
    }

    @Test
    void testExtractConfigGroupNotFound() {
        String yaml = "key: value\n";
        String configGroup = YamlUtils.extractConfigGroup(yaml);
        assertNull(configGroup);
    }

    @Test
    void testDetermineConfigType() {
        assertEquals("yaml", YamlUtils.determineConfigType("config.yml"));
        assertEquals("yaml", YamlUtils.determineConfigType("config.yaml"));
        assertEquals("json", YamlUtils.determineConfigType("config.json"));
        assertEquals("xml", YamlUtils.determineConfigType("config.xml"));
        assertEquals("properties", YamlUtils.determineConfigType("config.properties"));
        assertEquals("html", YamlUtils.determineConfigType("config.html"));
        assertEquals("text", YamlUtils.determineConfigType("config.txt"));
        assertEquals("text", YamlUtils.determineConfigType("config.unknown"));
    }

    @Test
    void testFormatYaml() {
        String formatted = YamlUtils.formatYaml(SAMPLE_YAML);
        assertNotNull(formatted);
        assertFalse(formatted.isEmpty());

        // 验证格式化后的 YAML 仍然可以解析
        Map<String, Object> parsed = YamlUtils.parseYaml(formatted);
        assertNotNull(parsed);
    }
}