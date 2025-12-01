# IntelliAI 插件套件 Makefile
# 为所有插件提供构建、运行、测试和发布的便捷操作

.PHONY: help build run test clean doc publish-install publish-repo verify check-format

# 插件目录
ENGINE_DIR := intelli-ai-engine
JAVADOC_DIR := intelli-ai-javadoc
CHANGELOG_DIR := intelli-ai-changelog
NACOS_DIR := intelli-ai-nacos
TRACER_DIR := intelli-ai-tracer

build-javadoc:
	@echo "正在构建 intelli-ai-javadoc 插件..."
	cd $(JAVADOC_DIR) && ./gradlew buildPlugin

build-engine:
	@echo "正在构建 intelli-ai-engine 插件..."
	cd $(ENGINE_DIR) && ./gradlew buildPlugin

build-changelog:
	@echo "正在构建 intelli-ai-changelog 插件..."
	cd $(CHANGELOG_DIR) && ./gradlew buildPlugin

build-nacos:
	@echo "正在构建 intelli-ai-nacos 插件..."
	cd $(NACOS_DIR) && ./gradlew buildPlugin

build-tracer:
	@echo "正在构建遗留的  intelli-ai-tracer..."
	cd $(TRACER_DIR) && ./gradlew buildPlugin

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

# 文档命令
doc: doc-javadoc doc-engine

doc-javadoc:
	@echo "正在生成 JavaDoc 文档..."
	cd $(JAVADOC_DIR) && ./gradlew javadoc

doc-engine:
	@echo "正在生成引擎文档..."
	cd $(ENGINE_DIR) && ./gradlew javadoc

# 发布命令
publish-engine:
	@echo "正在发布 intelli-ai-engine 插件..."
	cd $(ENGINE_DIR) && ./gradlew publishPlugin

publish-javadoc:
	@echo "正在发布 intelli-ai-javadoc 插件..."
	cd $(JAVADOC_DIR) && ./gradlew publishPlugin

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

# 清理命令
clean: clean-javadoc clean-engine clean-changelog clean-nacos clean-tracer
# 构建命令
build: build-javadoc build-engine build-changelog build-nacos  build-tracer

deploy-all: deploy-engine deploy-javadoc deploy-changelog deploy-nacos deploy-tracer

# 插件版本信息
version:
	@echo "插件版本："
	@cd $(ENGINE_DIR) && ./gradlew properties -q | grep version | grep -v kotlin
	@cd $(JAVADOC_DIR) && ./gradlew properties -q | grep version | grep -v kotlin
	@cd $(CHANGELOG_DIR) && ./gradlew properties -q | grep version | grep -v kotlin
	@cd $(TRACER_DIR) && ./gradlew properties -q | grep version | grep -v kotlin
	@cd $(NACOS_DIR) && ./gradlew properties -q | grep version | grep -v kotlin

