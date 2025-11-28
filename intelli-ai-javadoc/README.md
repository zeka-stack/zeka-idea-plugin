# IntelliJ IntelliAI JavaDoc Plugin

[中文](./README_CN.md) | **English** | [📄 Beautiful Landing Page](./index.html)

> 🌟 [View our beautiful landing page](https://aij.dong4j.site) - Get an intuitive overview of plugin features!


The IntelliJ IntelliAI JavaDoc Plugin is a powerful tool that utilizes artificial intelligence to generate JavaDoc comments for your Java
code. It
supports multiple AI service providers including QianWen (Alibaba Cloud) and Ollama local models, providing intelligent and context-aware
suggestions for documenting your methods, classes, and interfaces.

## Features

- **Multiple AI Providers**: Support for QianWen (Alibaba Cloud) and Ollama local models to meet different user needs
- **Automatic JavaDoc Generation**: Simplify the process of writing JavaDoc comments by letting the plugin generate them for you
- **Smart Element Recognition**: Intelligently identify elements to document based on cursor position (methods, fields, classes)
- **Context-aware Suggestions**: The plugin analyzes your code and takes into account the surrounding context to provide more precise and
  meaningful JavaDoc suggestions
- **Multiple Trigger Methods**: Support for shortcuts, right-click menu, Generate menu, and Intention Action
- **Batch Processing**: Support for generating documentation for entire files or multiple selected files
- **Incremental Updates**: Only generate documentation for classes and methods without existing JavaDoc comments
- **Test Method Recognition**: Automatically identify JUnit 4 and JUnit 5 test methods and use specialized templates
- **Configuration Validation**: Built-in connection testing to ensure proper configuration

## Installation

1. Launch IntelliJ IDEA.
2. Go to **Settings** (Preferences on macOS) -> **Plugins**.
3. Click on the **Marketplace** tab.
4. Search for "AIJavadoc" in the search bar.
5. Click **Install** next to the IntelliJ IntelliAI JavaDoc Plugin.
6. Restart IntelliJ IDEA to activate the plugin.

## Usage

The plugin provides multiple trigger methods, you can choose according to your needs:

### Shortcut Method (Recommended)

1. Open your Java project in IntelliJ IDEA
2. Place the cursor on the element you want to document (method, field, class)
3. Press **Ctrl + Shift + D** (Windows/Linux) or **Cmd + Shift + D** (macOS)
4. The plugin will intelligently identify and generate appropriate documentation based on cursor position

### Other Trigger Methods

- **Right-click Menu**: Right-click in editor → "Generate Javadoc with AI"
- **Generate Menu**: Press **Alt + Insert** (Windows/Linux) or **Cmd + N** (macOS) → Select "Generate Javadoc with AI"
- **Intention Action**: Press **Alt + Enter** (Windows/Linux) or **Option + Enter** (macOS) → Select "Generate Javadoc with AI"

### Smart Positioning

The plugin intelligently determines the generation scope based on cursor position:

- Cursor on method → Generate documentation only for that method
- Cursor on field → Generate documentation only for that field
- Cursor on class declaration → Generate documentation only for that class
- Cursor inside class (but not on specific member) → Generate documentation for entire class and all members
- Other cases → Generate documentation for entire file

## Configuration

The plugin supports multiple AI service providers. You can choose according to your needs:

### QianWen (Recommended)

QianWen is a large language model service provided by Alibaba Cloud, supporting Chinese and multilingual processing, suitable for Chinese
documentation generation.

**Configuration Steps:**

1. Visit [Alibaba Cloud DashScope Console](https://dashscope.console.aliyun.com/) to register an account
2. Get your API Key
3. In IntelliJ IDEA, go to **Settings** → **Tools** → **IntelliAI JavaDoc**
4. Select **AI Provider** as "qianwen"
5. Enter your **API Key**
6. Use default **Base URL**: `https://dashscope.aliyuncs.com/compatible-mode/v1`
7. **Model** select recommended models:
    - `qwen-max`: Most powerful model (recommended)
    - `qwen-plus`: Good cost-performance ratio
    - `qwen-turbo`: Fastest speed
8. Click **Test Connection** to verify configuration
9. Click **Apply** to save configuration

### Ollama (Local Models)

Ollama supports running open-source large language models locally, with complete data privacy and no API Key required.

**Configuration Steps:**

1. Install Ollama: Visit [https://ollama.ai](https://ollama.ai) to download and install
2. Pull model: `ollama pull qwen:7b` (recommended for Chinese)
3. Start service: `ollama serve`
4. In plugin settings, select **AI Provider** as "ollama"
5. Use default **Base URL**: `http://localhost:11434/v1`
6. Leave **API Key** empty (local service doesn't need it)
7. **Model** enter installed model name, e.g.: `qwen:7b`
8. Click **Test Connection** to verify configuration
9. Click **Apply** to save configuration

**Recommended Models:**

- `qwen:7b` / `qwen:14b`: QianWen series, good Chinese support
- `codellama:7b`: Code-specific model
- `deepseek-coder:6.7b`: DeepSeek code model
- `llama2:7b`: General model

### Custom Service (OpenAI Compatible)

Supports any custom service compatible with OpenAI API interface, including OpenAI official service, Azure OpenAI, third-party services,
etc.

**Configuration Steps:**

1. In plugin settings, select **AI Provider** as "custom"
2. **Base URL** enter service address, for example:
    - OpenAI: `https://api.openai.com/v1`
    - Azure OpenAI: `https://your-resource.openai.azure.com/openai/deployments/your-deployment`
    - Self-hosted service: `http://localhost:8000/v1`
3. **API Key** enter the service's API key
4. **Model** enter model name, for example:
    - `gpt-3.5-turbo`: OpenAI's fast model
    - `gpt-4`: OpenAI's high-quality model
    - `gpt-4-turbo`: OpenAI's latest model
    - `claude-3-sonnet`: Anthropic Claude model
    - `gemini-pro`: Google Gemini model
5. Click **Test Connection** to verify configuration
6. Click **Apply** to save configuration

**Supported Common Services:**

- **OpenAI**: Official OpenAI service
- **Azure OpenAI**: OpenAI service on Microsoft Azure
- **Anthropic Claude**: Use Claude models through compatibility layer
- **Google Gemini**: Use Gemini models through compatibility layer
- **Self-hosted Service**: Any self-hosted service compatible with OpenAI API

**Notes:**

- Ensure the service supports OpenAI compatible `/chat/completions` interface
- API Key format is usually "Bearer {key}" or use key directly
- Model name must match the models supported by the service provider
- Some services may have rate limits or usage quotas

### Advanced Configuration

In the settings panel, you can also configure:

- **Generation Options**: Choose to generate documentation for classes, methods, fields
- **Skip Existing Documentation**: Avoid overwriting existing JavaDoc
- **Language Support**: Currently supports Java (Kotlin coming soon)
- **Advanced Parameters**: Retry count, timeout, temperature parameters, etc.
- **Prompt Templates**: Customize documentation generation templates

## Limitations

- The plugin relies on the availability and performance of the selected AI service provider. Network connectivity and API rate limits may
  impact its functionality
- The accuracy and quality of the generated JavaDoc comments are dependent on the capabilities and limitations of the underlying AI model
- Using Ollama local models requires sufficient system resources (memory, CPU) to run the models
- QianWen service requires a valid API Key and network connection

## Contributing

Contributions to the IntelliJ IntelliAI JavaDoc Plugin are welcome! If you encounter any bugs, have feature requests, or would like to
contribute
code improvements, please submit an issue or pull request on the GitHub repository.

## Author

dong4j dong4j@gmail.com

---

## Related Documentation

- [IDEA Plugin Development Guide](docs/IDEA_Plugin_开发文档.md) - Complete guide for IntelliJ IDEA plugin development
- [Chinese README](README_CN.md) - Chinese version of this document

## Quick Start

### Development Environment Setup

1. Clone the project:
   ```bash
   git clone https://github.com/zeka-stack/zeka-idea-plugin.git
   cd zeka-idea-plugin/intelli-ai-javadoc
   ```

2. Build the project using Gradle:
   ```bash
   ./gradlew build
   ```

3. Run the plugin (start IDE with plugin):
   ```bash
   ./gradlew runIde
   ```

4. Run tests:
   ```bash
   ./gradlew test
   ```

### Project Structure

```
intelli-ai-javadoc/
├── src/main/java/com/github/intellijjavadocai/
│   ├── action/          # Action handlers (user interaction entry points)
│   ├── ai/              # AI service providers (QianWen, Ollama)
│   ├── config/          # Configuration management
│   ├── generator/       # JavaDoc generators
│   ├── service/         # Service layer (HTTP requests, Prompt management)
│   ├── settings/        # Settings and UI
│   ├── task/            # Task management (async processing)
│   └── ErrorHandler.java  # Error handling
├── src/main/resources/
│   ├── META-INF/
│   │   └── plugin.xml   # Plugin configuration file
│   ├── messages.properties     # English messages
│   └── messages_zh_CN.properties  # Chinese messages
└── build.gradle.kts     # Gradle build script
```

## Technology Stack

- **Java 11** - Development language
- **IntelliJ Platform SDK** - Plugin development framework
- **Spring Web** - HTTP client
- **QianWen API** - Alibaba Cloud AI documentation generation service
- **Ollama** - Local large language model runtime environment
- **Gradle** - Build tool
- **Lombok** - Simplify Java code
- **SLF4J** - Logging framework

## Core Features

### 1. Smart Test Method Recognition

The plugin automatically identifies JUnit 4 and JUnit 5 test methods and uses specialized templates to generate documentation that better
fits the characteristics of test methods.

### 2. Incremental Updates

Only generates documentation for classes and methods without existing JavaDoc comments, without overwriting existing comments.

### 3. Error Retry Mechanism

Uses exponential backoff strategy to handle API rate limiting and temporary failures, ensuring stability.

### 4. Complete IDE Integration

- Shortcut support
- Only available in Java files
- Automatically disabled during indexing

## FAQ

### Q: Why doesn't the plugin respond?

A: Please check the following:

1. Ensure AI provider is properly configured (QianWen or Ollama)
2. Confirm the current file is a Java file
3. Check if IDE is indexing (status bar shows indexing progress)
4. Check IDE logs for error messages
5. Ensure configuration has passed connection test

### Q: How to view API call failure reasons?

A: Check IDE log files:

- **macOS**: `~/Library/Logs/JetBrains/IntelliJIdea<Version>/idea.log`
- **Windows**: `%USERPROFILE%\AppData\Local\JetBrains\IntelliJIdea<Version>\log\idea.log`
- **Linux**: `~/.cache/JetBrains/IntelliJIdea<Version>/log/idea.log`

### Q: How to customize Prompt templates?

A: In the settings panel's "Prompt Template Configuration" section, you can customize:

- Class Prompt: Class documentation template
- Method Prompt: Method documentation template
- Field Prompt: Field documentation template
- Test Method Prompt: Test method documentation template
  Use `%s` as code placeholder

### Q: Which test frameworks are supported?

A: Currently supports:

- JUnit 4 (`@org.junit.Test`)
- JUnit 5 (`@org.junit.jupiter.api.Test`)

### Q: How to choose AI provider?

A: Choose based on your needs:

- **QianWen**: Suitable for Chinese documentation generation, requires API Key, has usage costs
- **Ollama**: Runs locally, data private, no API Key required, needs sufficient system resources

## License

Please check the LICENSE file in the project root directory.

## Support

If you encounter problems or have any questions, please:

1. Check [IDEA Plugin Development Guide](docs/IDEA_Plugin_开发文档.md)
2. Submit issues on [GitHub Issues](https://github.com/zeka-stack/zeka-idea-plugin/issues)
3. Send email to dong4j@gmail.com

## Acknowledgments

Thanks to the following service providers for making this project possible:

- **Alibaba Cloud QianWen** - Providing powerful Chinese AI services
- **Ollama** - Providing local large language model runtime environment
