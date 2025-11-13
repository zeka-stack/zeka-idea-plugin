package dev.dong4j.zeka.stack.idea.plugin.changelog.service;

import dev.dong4j.zeka.stack.idea.plugin.changelog.settings.SettingsState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * ChangelogService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class ChangelogServiceTest {

    @Mock
    private com.intellij.openapi.project.Project mockProject;

    private ChangelogService exampleService;

    @BeforeEach
    void setUp() {
        exampleService = new ChangelogService(mockProject);
    }

    @Test
    void testProcessText() {
        // Given
        String text = "Hello World";
        int offset = 5;

        // When
        String result = exampleService.processText(text, offset);

        // Then
        assertThat(result).contains("Processed text at offset 5");
        assertThat(result).contains("length: 11");
    }

    @Test
    void testProcessFile() {
        // Given
        com.intellij.psi.PsiFile mockFile = org.mockito.Mockito.mock(com.intellij.psi.PsiFile.class);
        when(mockFile.getName()).thenReturn("Test.java");
        when(mockFile.getText()).thenReturn("public class Test {}");

        // When
        String result = exampleService.processFile(mockFile);

        // Then
        assertThat(result).contains("Processed file: Test.java");
        assertThat(result).contains("size: 20 bytes");
    }

    @Test
    void testGetProjectInfo() {
        // Given
        when(mockProject.getName()).thenReturn("TestProject");
        when(mockProject.getBasePath()).thenReturn("/path/to/project");

        // When
        String result = exampleService.getProjectInfo();

        // Then
        assertThat(result).contains("Project: TestProject");
        assertThat(result).contains("Base Path: /path/to/project");
    }
}
