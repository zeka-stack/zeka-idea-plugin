package dev.dong4j.zeka.stack.idea.plugin.nacos.settings;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SettingsState 单元测试
 *
 * @author dong4j
 * @since 1.0.0
 */
class SettingsStateTest {

    private SettingsState settingsState;

    @BeforeEach
    void setUp() {
        settingsState = new SettingsState();
    }

    @Test
    void testInitialState() {
        assertNotNull(settingsState);
        assertEquals("", settingsState.serverAddr);
        assertEquals("", settingsState.username);
        assertEquals("YAML", settingsState.type);
        assertFalse(settingsState.globalAdmin);
        assertFalse(settingsState.isAuthed);
    }

    @Test
    void testSetAndGetServerAddr() {
        String serverAddr = "http://localhost:8848";
        settingsState.serverAddr = serverAddr;
        assertEquals(serverAddr, settingsState.serverAddr);
    }

    @Test
    void testSetAndGetUsername() {
        String username = "nacos";
        settingsState.username = username;
        assertEquals(username, settingsState.username);
    }

    @Test
    void testSetAndGetConfigType() {
        String type = "JSON";
        settingsState.type = type;
        assertEquals(type, settingsState.type);
    }

    @Test
    void testSetAndGetGlobalAdmin() {
        settingsState.globalAdmin = true;
        assertTrue(settingsState.globalAdmin);
    }

    @Test
    void testSetAndGetIsAuthed() {
        settingsState.isAuthed = true;
        assertTrue(settingsState.isAuthed);
    }

    @Test
    void testGetInstance() {
        SettingsState instance1 = SettingsState.getInstance();
        SettingsState instance2 = SettingsState.getInstance();
        assertSame(instance1, instance2);
    }

    @Test
    void testGetState() {
        assertSame(settingsState, settingsState.getState());
    }

    @Test
    void testLoadState() {
        SettingsState newState = new SettingsState();
        newState.serverAddr = "http://test:8848";
        newState.username = "testuser";

        settingsState.loadState(newState);

        assertEquals("http://test:8848", settingsState.serverAddr);
        assertEquals("testuser", settingsState.username);
    }
}