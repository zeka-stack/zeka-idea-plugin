# IntelliAI 插件套件 Makefile
# 为所有插件提供构建、运行、测试和发布的便捷操作

.PHONY: help build run test clean doc publish-install publish-repo verify check-format copy-zips

# 插件目录
ENGINE_DIR := intelli-ai-engine
JAVADOC_DIR := intelli-ai-javadoc
CHANGELOG_DIR := intelli-ai-changelog
NACOS_DIR := intelli-ai-nacos
TRACER_DIR := intelli-ai-tracer

# 构建产物输出目录
DIST_DIR := ~/Downloads/IntelliAI

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

# 拷贝构建产物到 Downloads 目录
copy-zips:
	@echo "正在拷贝构建产物到 $(DIST_DIR)..."
	@mkdir -p $(DIST_DIR)
	@for dir in $(ENGINE_DIR) $(JAVADOC_DIR) $(CHANGELOG_DIR) $(NACOS_DIR) $(TRACER_DIR); do \
		zip_file=$$(ls -t $$dir/build/distributions/$$dir-*.zip 2>/dev/null | head -n1); \
		if [ -n "$$zip_file" ]; then \
			echo "  拷贝 $$zip_file -> $(DIST_DIR)/$$(basename $$zip_file)"; \
			cp -f $$zip_file $(DIST_DIR)/; \
		else \
			echo "  警告: 未找到 $$dir 的构建产物"; \
		fi; \
	done
	@echo "✓ 构建产物拷贝完成"

# 清理命令
clean: clean-engine clean-javadoc clean-changelog clean-nacos clean-tracer
# 构建命令（包含拷贝构建产物）
build: build-engine build-javadoc  build-changelog build-nacos build-tracer copy-zips

# 子插件部署（可以并发执行，因为它们操作不同的目录和远程路径）
# 使用 make -j4 deploy-sub 可以并发执行 4 个任务
deploy-sub: deploy-javadoc deploy-changelog deploy-tracer deploy-nacos

# 插件版本信息
version:
	@echo "插件版本："
	@for dir in $(ENGINE_DIR) $(JAVADOC_DIR) $(CHANGELOG_DIR) $(TRACER_DIR) $(NACOS_DIR); do \
		version=$$(cd $$dir && ./gradlew properties -q | grep "^version:" | awk -F: '{print $$2}' | xargs); \
		printf "  %-25s %s\n" "$$dir:" "$$version"; \
	done

# GNU Make 官方推荐使用 $(MAKE) 进行递归调用
quick-clean:
	@echo "正在快速清理插件..."
	$(MAKE) -j5 clean
quick-build:
	@echo "正在快速构建插件..."
	$(MAKE) -j5 build

quick-deploy:
	@echo "正在快速部署插件..."
	$(MAKE) -j4 deploy-sub