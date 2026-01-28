# IntelliAI 插件套件 Makefile
# 为所有插件提供构建、运行、测试和发布的便捷操作

.PHONY: help build run test clean doc publish-install publish-repo verify check-format copy-zips install-plugins copy-and-install copy-install-upload copy-install-upload-publish-beta publish-swagger-beta publish-swagger-default

# 插件目录
KIT_DIR := idea-plugin-kit
ENGINE_DIR := intelli-ai-engine
JAVADOC_DIR := intelli-ai-javadoc
CHANGELOG_DIR := intelli-ai-changelog
NACOS_DIR := intelli-ai-nacos
TRACER_DIR := intelli-ai-tracer
REPAIRER_DIR := intelli-ai-repairer
TERMINAL_DIR := intelli-ai-terminal

# 构建产物输出目录
DIST_DIR := /Users/dong4j/Downloads/IntelliAI
IDEA_PLUGINS_DIR := /Users/dong4j/Developer/4.Tools/JetBrains/IDEA/plugins

build-engine:
	@echo "正在构建 intelli-ai-engine 插件..."
	cd $(ENGINE_DIR) && ./gradlew buildPlugin

build-javadoc:
	@echo "正在构建 intelli-ai-javadoc 插件..."
	cd $(JAVADOC_DIR) && ./gradlew buildPlugin

build-changelog:
	@echo "正在构建 intelli-ai-changelog 插件..."
	cd $(CHANGELOG_DIR) && ./gradlew buildPlugin

build-nacos:
	@echo "正在构建 intelli-ai-nacos 插件..."
	cd $(NACOS_DIR) && ./gradlew buildPlugin

build-tracer:
	@echo "正在构建遗留的  intelli-ai-tracer..."
	cd $(TRACER_DIR) && ./gradlew buildPlugin

build-repairer:
	@echo "正在构建 intelli-ai-repairer 插件..."
	cd $(REPAIRER_DIR) && ./gradlew buildPlugin

build-terminal:
	@echo "正在构建 intelli-ai-terminal 插件..."
	cd $(TERMINAL_DIR) && ./gradlew buildPlugin

# 运行命令
run: run-javadoc

run-javadoc:
	@echo "正在沙箱IDE中运行 intelli-ai-javadoc..."
	cd $(JAVADOC_DIR) && ./gradlew runIde

clean-engine:
	@echo "正在清理 intelli-ai-engine..."
	cd $(ENGINE_DIR) && ./gradlew clean

clean-javadoc:
	@echo "正在清理 intelli-ai-javadoc..."
	cd $(JAVADOC_DIR) && ./gradlew clean

clean-changelog:
	@echo "正在清理 intelli-ai-changelog..."
	cd $(CHANGELOG_DIR) && ./gradlew clean

clean-nacos:
	@echo "正在清理 intelli-ai-nacos..."
	cd $(NACOS_DIR) && ./gradlew clean

clean-tracer:
	@echo "正在清理 intelli-ai-tracer..."
	cd $(TRACER_DIR) && ./gradlew clean

clean-repairer:
	@echo "正在清理 intelli-ai-repairer..."
	cd $(REPAIRER_DIR) && ./gradlew clean

clean-terminal:
	@echo "正在清理 intelli-ai-terminal..."
	cd $(TERMINAL_DIR) && ./gradlew clean

# 文档命令
doc: doc-javadoc doc-engine

doc-javadoc:
	@echo "正在生成 Javadoc 文档..."
	cd $(JAVADOC_DIR) && ./gradlew javadoc

doc-engine:
	@echo "正在生成引擎文档..."
	cd $(ENGINE_DIR) && ./gradlew javadoc

# 本地安装 kit
install-kit:
	cd $(KIT_DIR) && ./gradlew publishToMavenLocal

install-engine:
	cd $(ENGINE_DIR) && ./gradlew publishToMavenLocal

install-dependencies: install-kit install-engine

# 发布命令
publish-engine:
	@echo "正在发布 intelli-ai-engine 插件..."
	cd $(ENGINE_DIR) && ./gradlew publishDefault
	@echo "正在完成: https://plugins.jetbrains.com/plugin/29152."

publish-javadoc:
	@echo "正在发布 intelli-ai-javadoc 插件..."
	cd $(JAVADOC_DIR) && ./gradlew publishPlugin
	@echo "正在完成: https://plugins.jetbrains.com/plugin/28835."

publish-swagger-beta:
	@echo "正在发布 intelli-ai-swagger (beta)..."
	cd intelli-ai-swagger && ./gradlew publishBeta

publish-swagger-default:
	@echo "正在发布 intelli-ai-swagger (default)..."
	cd intelli-ai-swagger && ./gradlew publishDefault

# 必须先部署且通过审核才能发布后续插件
deploy-engine:
	@echo "正在部署 intelli-ai-engine 插件..."
	./deploy.sh engine

deploy-javadoc:
	@echo "正在部署 intelli-ai-javadoc 插件..."
	./deploy.sh javadoc

deploy-changelog:
	@echo "正在部署 intelli-ai-changelog 插件..."
	./deploy.sh changelog

deploy-nacos:
	@echo "正在部署 intelli-ai-nacos 插件..."
	./deploy.sh nacos

deploy-tracer:
	@echo "正在部署 intelli-ai-tracer 插件..."
	./deploy.sh tracer

deploy-repairer:
	@echo "正在部署 intelli-ai-repairer 插件..."
	./deploy.sh repairer

deploy-terminal:
	@echo "正在部署 intelli-ai-terminal 插件..."
	./deploy.sh terminal

# 清理命令
clean: clean-engine clean-javadoc clean-changelog clean-nacos clean-tracer
# 构建命令（包含拷贝构建产物）
build: build-javadoc  build-changelog build-nacos build-tracer build-repairer build-terminal copy-zips

# 子插件部署（可以并发执行，因为它们操作不同的目录和远程路径）
# 使用 make -j4 deploy-sub 可以并发执行 4 个任务
deploy-sub: deploy-javadoc deploy-changelog deploy-tracer deploy-nacos deploy-repairer deploy-terminal

# 插件版本信息
version:
	@echo "插件版本:"
	@for dir in $(ENGINE_DIR) $(JAVADOC_DIR) $(CHANGELOG_DIR) $(TRACER_DIR) $(NACOS_DIR) $(REPAIRER_DIR) $(TERMINAL_DIR); do \
		version=$$(cd $$dir && ./gradlew properties -q | grep "pluginVersion" | awk -F: '{print $$2}' | xargs); \
		printf "  %-25s %s\n" "$$dir:" "$$version"; \
	done

# GNU Make 官方推荐使用 $(MAKE) 进行递归调用
quick-clean:
	@echo "正在快速清理插件..."
	$(MAKE) -j7 clean

# 必须先构建 engine
quick-build: build-engine
	@echo "正在快速构建插件..."
	$(MAKE) -j6 build

quick-deploy:
	@echo "正在快速部署插件..."
	$(MAKE) -j4 deploy-sub

# 拷贝构建产物到指定目录（带版本号）
# 用法: make copy-zips [TARGET_DIR=/path/to/dir]
copy-zips: build-engine build-javadoc  build-changelog build-nacos build-tracer build-terminal
	@BASE_TARGET=$${TARGET_DIR:-$(DIST_DIR)}; \
	version=$$(cd $(ENGINE_DIR) && ./gradlew properties -q | grep "pluginVersion" | awk -F: '{print $$2}' | xargs); \
	if [ -z "$$version" ]; then \
		echo "错误: 无法读取版本号"; \
		exit 1; \
	fi; \
	TARGET=$$BASE_TARGET/$$version; \
	echo "正在拷贝构建产物到 $$TARGET (版本: $$version)..."; \
	mkdir -p $$TARGET; \
	for dir in $(ENGINE_DIR) $(JAVADOC_DIR) $(CHANGELOG_DIR) $(NACOS_DIR) $(TRACER_DIR) $(TERMINAL_DIR); do \
		zip_file=$$(ls -t $$dir/build/distributions/$$dir-*.zip 2>/dev/null | head -n1); \
		if [ -n "$$zip_file" ]; then \
			echo "  拷贝 $$zip_file -> $$TARGET/$$(basename $$zip_file)"; \
			cp -f $$zip_file $$TARGET/; \
		else \
			echo "  警告: 未找到 $$dir 的构建产物"; \
		fi; \
	done; \
	echo "✓ 构建产物拷贝完成"; \
	echo "正在 Finder 中打开 $$TARGET..."; \
	open $$TARGET

# 拷贝构建产物到 IDEA 插件目录（解压后拷贝目录）
# 复用 copy-zips 的结果，从目标目录读取所有 zip 文件并安装
# 用法: make install-plugins [TARGET_DIR=/path/to/dir]
install-plugins: copy-zips
	@BASE_SOURCE_DIR=$${TARGET_DIR:-$(DIST_DIR)}; \
	version=$$(cd $(ENGINE_DIR) && ./gradlew properties -q | grep "^version:" | awk -F: '{print $$2}' | xargs); \
	if [ -z "$$version" ]; then \
		echo "错误: 无法读取版本号"; \
		exit 1; \
	fi; \
	SOURCE_DIR=$$BASE_SOURCE_DIR/$$version; \
	TARGET=$(IDEA_PLUGINS_DIR); \
	echo "正在从 $$SOURCE_DIR 安装插件到 $$TARGET (版本: $$version)..."; \
	mkdir -p $$TARGET; \
	for zip_file in $$SOURCE_DIR/*.zip; do \
		if [ -f "$$zip_file" ]; then \
			temp_dir=$$(mktemp -d); \
			echo "  解压 $$(basename $$zip_file)..."; \
			unzip -q -o $$zip_file -d $$temp_dir; \
			plugin_dir=$$(find $$temp_dir -maxdepth 1 -type d ! -path $$temp_dir | head -n1); \
			if [ -n "$$plugin_dir" ] && [ -d $$plugin_dir ]; then \
				plugin_name=$$(basename $$plugin_dir); \
				target_plugin_dir=$$TARGET/$$plugin_name; \
				echo "  安装 $$plugin_name -> $$target_plugin_dir"; \
				rm -rf $$target_plugin_dir; \
				mv $$plugin_dir $$target_plugin_dir; \
			else \
				echo "  警告: 解压后未找到插件目录 ($$zip_file)"; \
			fi; \
			rm -rf $$temp_dir; \
		fi; \
	done; \
	echo "✓ 插件安装完成,请重启 IDEA 以应用更改"

# 阿里云服务器配置
ALIYUN_HOST := aliyun
ALIYUN_PLUGIN_DIR := /var/www/data/intelli-ai-plugin

# 拷贝构建产物到指定目录，安装到 IDEA 插件目录，然后上传到阿里云
# 用法: make copy-install-upload [TARGET_DIR=/path/to/dir]
copy-install-upload: install-plugins
	@BASE_TARGET=$${TARGET_DIR:-$(DIST_DIR)}; \
	version=$$(cd $(ENGINE_DIR) && ./gradlew properties -q | grep "^version:" | awk -F: '{print $$2}' | xargs); \
	if [ -z "$$version" ]; then \
		echo "错误: 无法读取版本号"; \
		exit 1; \
	fi; \
	TARGET=$$BASE_TARGET/$$version; \
	echo ""; \
	echo "正在上传构建产物到阿里云服务器..."; \
	echo "目标服务器: $(ALIYUN_HOST)"; \
	echo "目标目录: $(ALIYUN_PLUGIN_DIR)"; \
	echo "版本: $$version"; \
	if [ ! -d "$$TARGET" ]; then \
		echo "错误: 目标目录不存在: $$TARGET"; \
		exit 1; \
	fi; \
	ssh $(ALIYUN_HOST) "mkdir -p $(ALIYUN_PLUGIN_DIR)"; \
	for zip_file in $$TARGET/*.zip; do \
		if [ -f "$$zip_file" ]; then \
			echo "  上传 $$(basename $$zip_file) -> $(ALIYUN_HOST):$(ALIYUN_PLUGIN_DIR)/"; \
			rsync -avz --progress "$$zip_file" "$(ALIYUN_HOST):$(ALIYUN_PLUGIN_DIR)/"; \
		fi; \
	done; \
	echo "✓ 构建产物上传完成"

# 拷贝、安装、上传，并发布 beta 渠道
copy-install-upload-publish-beta: copy-install-upload
	@echo "正在发布所有插件到 beta 渠道..."
	cd $(ENGINE_DIR) && ./gradlew publishBeta
	cd $(JAVADOC_DIR) && ./gradlew publishBeta
	cd $(CHANGELOG_DIR) && ./gradlew publishBeta
	cd $(NACOS_DIR) && ./gradlew publishBeta
	cd $(TRACER_DIR) && ./gradlew publishBeta
	cd $(REPAIRER_DIR) && ./gradlew publishBeta
	cd $(TERMINAL_DIR) && ./gradlew publishBeta
	cd intelli-ai-swagger && ./gradlew publishBeta
	cd archiver-man && ./gradlew publishBeta
	cd zks-dev-helper && ./gradlew publishBeta
	@echo "✓ beta 发布完成"
