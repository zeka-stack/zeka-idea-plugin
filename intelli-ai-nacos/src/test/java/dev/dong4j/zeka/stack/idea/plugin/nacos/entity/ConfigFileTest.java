package dev.dong4j.zeka.stack.idea.plugin.nacos.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ConfigFile 单元测试
 *
 * @author dong4j
 * @since 1.0.0
 */
class ConfigFileTest {

    @Test
    void testDefaultConstructor() {
        ConfigFile configFile = new ConfigFile();
        assertNotNull(configFile);
    }

    @Test
    void testConstructorWithParameters() {
        String namespace = "test-namespace";
        String group = "test-group";
        String dataId = "test-dataId";

        ConfigFile configFile = new ConfigFile(namespace, group, dataId);
        assertEquals(namespace, configFile.getNamespace());
        assertEquals(group, configFile.getGroup());
        assertEquals(dataId, configFile.getDataId());
    }

    @Test
    void testGettersAndSetters() {
        ConfigFile configFile = new ConfigFile();

        // Test namespace
        configFile.setNamespace("test-namespace");
        assertEquals("test-namespace", configFile.getNamespace());

        // Test group
        configFile.setGroup("test-group");
        assertEquals("test-group", configFile.getGroup());

        // Test dataId
        configFile.setDataId("test-dataId");
        assertEquals("test-dataId", configFile.getDataId());

        // Test content
        configFile.setContent("test-content");
        assertEquals("test-content", configFile.getContent());

        // Test type
        configFile.setType("yaml");
        assertEquals("yaml", configFile.getType());

        // Test lastModified
        long timestamp = System.currentTimeMillis();
        configFile.setLastModified(timestamp);
        assertEquals(timestamp, configFile.getLastModified());

        // Test appName
        configFile.setAppName("test-app");
        assertEquals("test-app", configFile.getAppName());
    }

    @Test
    void testGetUniqueId() {
        ConfigFile configFile = new ConfigFile("namespace", "group", "dataId");
        assertEquals("namespace:group:dataId", configFile.getUniqueId());
    }

    @Test
    void testFromFileNameWithEnvironment() {
        ConfigFile configFile = ConfigFile.fromFileName("myapp-dev.yml", "test-namespace");
        assertNotNull(configFile);
        assertEquals("test-namespace", configFile.getNamespace());
        assertEquals("myapp", configFile.getAppName());
        assertEquals("myapp.yml", configFile.getDataId());
        assertEquals("dev", configFile.getGroup());
        assertEquals("yaml", configFile.getType());
    }

    @Test
    void testFromFileNameWithoutEnvironment() {
        ConfigFile configFile = ConfigFile.fromFileName("myapp.yml", "test-namespace");
        assertNotNull(configFile);
        assertEquals("test-namespace", configFile.getNamespace());
        assertEquals("myapp", configFile.getAppName());
        assertEquals("myapp.yml", configFile.getDataId());
        assertEquals("DEFAULT_GROUP", configFile.getGroup());
        assertEquals("yaml", configFile.getType());
    }

    @Test
    void testFromFileNameWithDifferentExtensions() {
        // Test JSON
        ConfigFile jsonConfig = ConfigFile.fromFileName("config.json", "namespace");
        assertEquals("json", jsonConfig.getType());

        // Test XML
        ConfigFile xmlConfig = ConfigFile.fromFileName("config.xml", "namespace");
        assertEquals("xml", xmlConfig.getType());

        // Test Properties
        ConfigFile propConfig = ConfigFile.fromFileName("config.properties", "namespace");
        assertEquals("properties", propConfig.getType());

        // Test HTML
        ConfigFile htmlConfig = ConfigFile.fromFileName("config.html", "namespace");
        assertEquals("html", htmlConfig.getType());

        // Test TXT
        ConfigFile txtConfig = ConfigFile.fromFileName("config.txt", "namespace");
        assertEquals("text", txtConfig.getType());
    }

    @Test
    void testEqualsAndHashCode() {
        ConfigFile configFile1 = new ConfigFile("namespace", "group", "dataId");
        ConfigFile configFile2 = new ConfigFile("namespace", "group", "dataId");
        ConfigFile configFile3 = new ConfigFile("namespace", "group", "different");

        assertEquals(configFile1, configFile2);
        assertNotEquals(configFile1, configFile3);
        assertEquals(configFile1.hashCode(), configFile2.hashCode());
    }

    @Test
    void testToString() {
        ConfigFile configFile = new ConfigFile("namespace", "group", "dataId");
        configFile.setType("yaml");
        configFile.setAppName("test-app");

        String toStringResult = configFile.toString();
        assertNotNull(toStringResult);
        assertTrue(toStringResult.contains("namespace"));
        assertTrue(toStringResult.contains("group"));
        assertTrue(toStringResult.contains("dataId"));
        assertTrue(toStringResult.contains("yaml"));
        assertTrue(toStringResult.contains("test-app"));
    }
}