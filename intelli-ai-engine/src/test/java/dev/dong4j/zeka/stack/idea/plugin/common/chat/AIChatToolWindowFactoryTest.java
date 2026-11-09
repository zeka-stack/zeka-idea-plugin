/*
 * 聊天工具窗口的类加载边界回归测试，覆盖宿主未提供 JCEF 的环境。
 */
package dev.dong4j.zeka.stack.idea.plugin.common.chat;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import dev.dong4j.zeka.stack.idea.plugin.common.util.JcefSupport;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import javax.swing.JComponent;
import java.io.IOException;
import java.io.InputStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证降级 UI 不依赖聊天面板的类加载，避免构造函数内的探测来不及执行。
 */
class AIChatToolWindowFactoryTest {

    /**
     * 即使聊天面板完全无法加载，缺少 JCEF 时也必须能创建提示内容。
     *
     * @throws Exception 独立加载工具窗口工厂失败时使测试失败
     */
    @Test
    void createsFallbackWithoutLoadingChatPanelWhenJcefIsUnavailable() throws Exception {
        Project project = mock(Project.class);
        ToolWindow toolWindow = mock(ToolWindow.class);
        ContentManager contentManager = mock(ContentManager.class);
        ContentFactory contentFactory = mock(ContentFactory.class);
        Content content = mock(Content.class);
        when(toolWindow.getContentManager()).thenReturn(contentManager);
        when(contentFactory.createContent(any(JComponent.class), eq(""), eq(false))).thenReturn(content);

        PanelBlockingClassLoader loader = new PanelBlockingClassLoader();
        try (MockedStatic<ContentFactory> factory = mockStatic(ContentFactory.class)) {
            factory.when(ContentFactory::getInstance).thenReturn(contentFactory);

            ToolWindowFactory windowFactory = (ToolWindowFactory) loader
                .loadClass(AIChatToolWindowFactory.class.getName()).getDeclaredConstructor().newInstance();
            windowFactory.createToolWindowContent(project, toolWindow);

            verify(contentFactory).createContent(any(JComponent.class), eq(""), eq(false));
            verify(contentManager).addContent(content);
        }
    }

    /**
     * 隔离加载被测工厂，并禁止提前解析面板，重现缺少 JCEF 时的加载边界。
     */
    private static final class PanelBlockingClassLoader extends ClassLoader {

        /**
         * 其余平台类型共享测试运行时，确保 mock 和工厂使用相同的接口类型。
         */
        private PanelBlockingClassLoader() {
            super(AIChatToolWindowFactoryTest.class.getClassLoader());
        }

        /**
         * 隐藏 JCEF 并隔离加载探测工具；对面板的任何加载尝试均直接失败。
         *
         * @param name    待加载类的全限定名
         * @param resolve 是否解析加载的类
         * @return 已加载的类
         * @throws ClassNotFoundException 工厂编译产物不存在时抛出
         */
        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (name.startsWith("com.intellij.ui.jcef.") || name.startsWith("org.cef.")) {
                throw new ClassNotFoundException(name);
            }
            if (name.equals("dev.dong4j.zeka.stack.idea.plugin.common.chat.AIChatPanel")) {
                throw new AssertionError("缺少 JCEF 时不应加载 AIChatPanel");
            }
            if (name.equals(AIChatToolWindowFactory.class.getName()) || name.equals(JcefSupport.class.getName())) {
                Class<?> loaded = findLoadedClass(name);
                if (loaded == null) {
                    // IntelliJ 测试加载器没有稳定的 CodeSource URL，直接读取编译后的类资源。
                    try (InputStream stream = getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
                        if (stream == null) {
                            throw new ClassNotFoundException(name);
                        }
                        byte[] bytes = stream.readAllBytes();
                        loaded = defineClass(name, bytes, 0, bytes.length);
                    } catch (IOException exception) {
                        throw new ClassNotFoundException(name, exception);
                    }
                }
                if (resolve) {
                    resolveClass(loaded);
                }
                return loaded;
            }
            return super.loadClass(name, resolve);
        }
    }
}
