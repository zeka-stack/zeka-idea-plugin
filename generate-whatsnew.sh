#!/usr/bin/env bash
# 生成 IntelliAI 平台的聚合更新日志页面
#
# 功能说明：
# 1. 扫描整个项目，查找所有插件的 pluginChanges.html 文件
# 2. 验证每个文件对应的目录是否为有效的插件目录（包含 plugin.xml）
# 3. 从 plugin.xml 中提取插件名称
# 4. 生成一个聚合的 HTML 页面，包含所有插件的更新日志
# 5. 生成的页面使用现代化的深色主题样式，每个插件以卡片形式展示
# 6. 解析 Engine 插件的最新版本号（从 intelli-ai-engine/includes/pluginChanges.html）
# 7. 提取最新版本的中英文更新说明，创建版本文件（如 2025_3_1.html）
# 8. 自动更新 InternalWhatsNewProvider.java，添加版本映射关系
#
# 输出文件：
# 1. 主目录下的 latest.html（聚合所有插件的更新日志）
# 2. intelli-ai-engine/src/main/resources/whatsnew/latest.html（自动拷贝）
#    该文件用于在 IDE 中显示 "What's New" 对话框
# 3. intelli-ai-engine/src/main/resources/whatsnew/{version}.html（如 2025_3_1.html）
#    该文件包含 Engine 插件特定版本的更新说明，用于 IDE 帮助菜单中的版本历史
#
# 自动更新：
# - InternalWhatsNewProvider.java：在 // version mark 注释后自动添加新版本映射

set -euo pipefail

# 获取脚本所在目录和项目根目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$SCRIPT_DIR"
ENGINE_DIR="$ROOT_DIR/intelli-ai-engine"
OUTPUT_FILE="$ROOT_DIR/latest.html"
TARGET_DIR="$ENGINE_DIR/src/main/resources/whatsnew"

# 白名单：包含在 What's New 页面中的插件目录名
# 按照此列表的顺序生成 release notes
ALLOWED_PLUGINS=(
  "intelli-ai-engine"
  "intelli-ai-javadoc"
  "intelli-ai-changelog"
  "intelli-ai-nacos"
  # "intelli-ai-swagger"
  "intelli-ai-tracer"
)

# 检查插件是否在白名单中
is_allowed() {
  local plugin_dir="$1"
  local plugin_name="$(basename "$plugin_dir")"
  for allowed in "${ALLOWED_PLUGINS[@]}"; do
    if [[ "$plugin_name" == "$allowed" ]]; then
      return 0  # 在白名单中
    fi
  done
  return 1  # 不在白名单中
}

# 获取插件在白名单中的索引位置（用于排序）
get_plugin_order() {
  local plugin_name="$1"
  local index=0
  for allowed in "${ALLOWED_PLUGINS[@]}"; do
    if [[ "$plugin_name" == "$allowed" ]]; then
      echo "$index"
      return 0
    fi
    index=$((index + 1))
  done
  # 如果不在白名单中，返回一个很大的数字，确保排在最后
  echo "9999"
}

# 获取插件的主页链接
get_plugin_home_url() {
  local plugin_name="$1"
  case "$plugin_name" in
    "intelli-ai-engine") echo "https://ideaplugin.dong4j.site/engine/landing.html" ;;
    "intelli-ai-javadoc") echo "https://ideaplugin.dong4j.site/javadoc/landing.html" ;;
    "intelli-ai-changelog") echo "https://ideaplugin.dong4j.site/changelog/landing.html" ;;
    "intelli-ai-nacos") echo "https://ideaplugin.dong4j.site/nacos/landing.html" ;;
    "intelli-ai-swagger") echo "https://ideaplugin.dong4j.site/swagger/landing.html" ;;
    "intelli-ai-tracer") echo "https://ideaplugin.dong4j.site/tracer/landing.html" ;;
    *) echo "" ;;
  esac
}

# 获取插件的文档链接
get_plugin_docs_url() {
  local plugin_name="$1"
  case "$plugin_name" in
    "intelli-ai-engine") echo "https://ideaplugin.dong4j.site/engine/docs.html" ;;
    "intelli-ai-javadoc") echo "https://ideaplugin.dong4j.site/javadoc/docs.html" ;;
    "intelli-ai-changelog") echo "https://ideaplugin.dong4j.site/changelog/docs.html" ;;
    "intelli-ai-nacos") echo "https://ideaplugin.dong4j.site/nacos/docs.html" ;;
    "intelli-ai-swagger") echo "https://ideaplugin.dong4j.site/swagger/docs.html" ;;
    "intelli-ai-tracer") echo "https://ideaplugin.dong4j.site/tracer/docs.html" ;;
    *) echo "" ;;
  esac
}

# 获取插件的离线安装链接
get_plugin_download_url() {
  local plugin_name="$1"
  case "$plugin_name" in
    "intelli-ai-engine") echo "https://ideaplugin.dong4j.site/engine/intelli-ai-engine.zip" ;;
    "intelli-ai-javadoc") echo "https://ideaplugin.dong4j.site/javadoc/intelli-ai-javadoc.zip" ;;
    "intelli-ai-changelog") echo "https://ideaplugin.dong4j.site/changelog/intelli-ai-changelog.zip" ;;
    "intelli-ai-nacos") echo "https://ideaplugin.dong4j.site/nacos/intelli-ai-nacos.zip" ;;
    "intelli-ai-swagger") echo "https://ideaplugin.dong4j.site/swagger/intelli-ai-swagger.zip" ;;
    "intelli-ai-tracer") echo "https://ideaplugin.dong4j.site/tracer/intelli-ai-tracer.zip" ;;
    *) echo "" ;;
  esac
}

# 查找所有插件的 pluginChanges.html 文件
# 这些文件通常位于：{插件目录}/includes/pluginChanges.html
mapfile -d '' CHANGE_FILES < <(find "$ROOT_DIR" -type f -path "*/includes/pluginChanges.html" -print0)

# 验证每个文件对应的目录是否为有效的插件目录
# 通过检查是否存在 plugin.xml 文件来确认，并且只处理白名单中的插件
plugin_dirs=()
for change_file in "${CHANGE_FILES[@]}"; do
  # 获取插件目录：pluginChanges.html 位于 includes/ 目录下，需要向上两级
  plugin_dir="$(dirname "$(dirname "$change_file")")"
  # 验证是否为有效的插件目录（包含 plugin.xml）且在白名单中
  if [[ -f "$plugin_dir/src/main/resources/META-INF/plugin.xml" ]] && is_allowed "$plugin_dir"; then
    plugin_dirs+=("$plugin_dir")
  fi
done

# 如果没有找到任何插件目录，退出并报错
if [[ ${#plugin_dirs[@]} -eq 0 ]]; then
  echo "No pluginChanges.html files found in allowed plugins." >&2
  exit 1
fi

# 按照白名单的顺序对插件目录进行排序
# 创建一个临时文件来存储排序后的目录
sorted_dirs=$(for plugin_dir in "${plugin_dirs[@]}"; do
  plugin_name="$(basename "$plugin_dir")"
  order=$(get_plugin_order "$plugin_name")
  printf "%04d|%s\n" "$order" "$plugin_dir"
done | sort -t'|' -k1,1n | cut -d'|' -f2-)

# 获取当前时间，用于在生成的页面中显示生成时间
generated_at=$(date "+%Y-%m-%d %H:%M")

{
  cat <<'HTML'
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>IntelliAI What's New</title>
  <style>
    :root {
      --bg: #0f1115;
      --bg-accent: #1a1f2b;
      --surface: #141824;
      --surface-strong: #1c2232;
      --text: #f6f7fb;
      --muted: #b0b7c3;
      --accent: #4dd3ff;
      --accent-strong: #16a6d1;
      --glow: rgba(77, 211, 255, 0.35);
      --border: rgba(255, 255, 255, 0.08);
      --shadow: 0 24px 60px rgba(0, 0, 0, 0.35);
      --radius: 18px;
    }

    * { box-sizing: border-box; }

    body {
      margin: 0;
      font-family: "Manrope", "IBM Plex Sans", "SF Pro Display", "Segoe UI", sans-serif;
      background: radial-gradient(1200px circle at 10% 10%, rgba(77, 211, 255, 0.12), transparent 40%),
                  radial-gradient(800px circle at 90% 20%, rgba(141, 106, 255, 0.18), transparent 45%),
                  linear-gradient(135deg, var(--bg), var(--bg-accent));
      color: var(--text);
      min-height: 100vh;
    }

    .hero {
      padding: 48px 56px 24px 56px;
      border-bottom: 1px solid var(--border);
      backdrop-filter: blur(12px);
    }

    .hero h1 {
      font-size: 32px;
      margin: 0 0 10px 0;
      letter-spacing: 0.5px;
    }

    .hero p {
      margin: 0;
      color: var(--muted);
      font-size: 14px;
      max-width: 720px;
    }

    .meta {
      margin-top: 16px;
      font-size: 12px;
      color: var(--muted);
      display: inline-flex;
      gap: 12px;
      align-items: center;
      padding: 6px 12px;
      border: 1px solid var(--border);
      border-radius: 999px;
      background: rgba(255, 255, 255, 0.03);
    }

    main {
      padding: 28px 56px 60px 56px;
      display: grid;
      gap: 20px;
    }

    .card {
      background: linear-gradient(135deg, rgba(255, 255, 255, 0.02), rgba(255, 255, 255, 0.01));
      border: 1px solid var(--border);
      border-radius: var(--radius);
      box-shadow: var(--shadow);
      padding: 24px;
      position: relative;
      overflow: hidden;
      cursor: pointer;
      transition: all 0.3s ease;
    }

    .card:hover {
      border-color: rgba(77, 211, 255, 0.4);
      box-shadow: 0 24px 60px rgba(77, 211, 255, 0.15);
      transform: translateY(-2px);
    }

    .card::before {
      content: "";
      position: absolute;
      top: -40px;
      right: -60px;
      width: 180px;
      height: 180px;
      background: radial-gradient(circle, rgba(77, 211, 255, 0.25), transparent 70%);
      opacity: 0.7;
      pointer-events: none;
    }

    .card-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      margin-bottom: 16px;
    }

    .card-header-left {
      display: flex;
      align-items: baseline;
      gap: 12px;
      flex: 1;
    }

    .card-title {
      font-size: 18px;
      font-weight: 600;
      letter-spacing: 0.4px;
    }

    .card-meta {
      color: var(--muted);
      font-size: 12px;
    }

    .card-header-actions {
      display: flex;
      gap: 6px;
      align-items: center;
    }

    .card-body {
      color: var(--text);
      font-size: 14px;
      line-height: 1.6;
      max-height: 200px;
      overflow: hidden;
      position: relative;
    }

    .card-body::after {
      content: "";
      position: absolute;
      bottom: 0;
      left: 0;
      right: 0;
      height: 60px;
      background: linear-gradient(to bottom, transparent, var(--surface));
      pointer-events: none;
    }

    .card-body.expanded {
      max-height: none;
    }

    .card-body.expanded::after {
      display: none;
    }

    .card-footer {
      margin-top: 12px;
      text-align: center;
      color: var(--accent);
      font-size: 12px;
      font-weight: 500;
      opacity: 0.8;
      transition: opacity 0.2s;
    }

    .card:hover .card-footer {
      opacity: 1;
    }

    .card-button {
      padding: 4px 10px;
      border-radius: 4px;
      font-size: 11px;
      font-weight: 500;
      text-align: center;
      text-decoration: none;
      cursor: pointer;
      transition: all 0.2s ease;
      display: inline-block;
      white-space: nowrap;
      border: 1px solid;
    }

    .card-button-home {
      background: rgba(77, 211, 255, 0.1);
      border-color: rgba(77, 211, 255, 0.3);
      color: #4dd3ff;
    }

    .card-button-home:hover {
      background: rgba(77, 211, 255, 0.2);
      border-color: rgba(77, 211, 255, 0.5);
      color: #4dd3ff;
      transform: translateY(-1px);
    }

    .card-button-docs {
      background: rgba(74, 222, 128, 0.1);
      border-color: rgba(74, 222, 128, 0.3);
      color: #4ade80;
    }

    .card-button-docs:hover {
      background: rgba(74, 222, 128, 0.2);
      border-color: rgba(74, 222, 128, 0.5);
      color: #4ade80;
      transform: translateY(-1px);
    }

    .card-button-download {
      background: rgba(245, 158, 11, 0.1);
      border-color: rgba(245, 158, 11, 0.3);
      color: #f59e0b;
    }

    .card-button-download:hover {
      background: rgba(245, 158, 11, 0.2);
      border-color: rgba(245, 158, 11, 0.5);
      color: #f59e0b;
      transform: translateY(-1px);
    }

    .card-button:active {
      transform: translateY(0);
    }

    /* 弹窗样式 */
    .modal {
      display: none;
      position: fixed;
      z-index: 1000;
      left: 0;
      top: 0;
      width: 100%;
      height: 100%;
      background: rgba(0, 0, 0, 0.75);
      backdrop-filter: blur(8px);
      animation: fadeIn 0.2s ease;
    }

    .modal.active {
      display: flex;
      align-items: center;
      justify-content: center;
    }

    @keyframes fadeIn {
      from { opacity: 0; }
      to { opacity: 1; }
    }

    .modal-content {
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: var(--radius);
      box-shadow: var(--shadow);
      max-width: 900px;
      max-height: 85vh;
      width: 90%;
      padding: 32px;
      position: relative;
      overflow-y: auto;
      animation: slideUp 0.3s ease;
    }

    @keyframes slideUp {
      from {
        transform: translateY(30px);
        opacity: 0;
      }
      to {
        transform: translateY(0);
        opacity: 1;
      }
    }

    .modal-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 24px;
      padding-bottom: 16px;
      border-bottom: 1px solid var(--border);
    }

    .modal-title {
      font-size: 24px;
      font-weight: 600;
      margin: 0;
    }

    .modal-close {
      background: none;
      border: none;
      color: var(--muted);
      font-size: 28px;
      cursor: pointer;
      padding: 0;
      width: 32px;
      height: 32px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 6px;
      transition: all 0.2s;
    }

    .modal-close:hover {
      background: rgba(255, 255, 255, 0.1);
      color: var(--text);
    }

    .modal-body {
      color: var(--text);
      font-size: 14px;
      line-height: 1.6;
    }

    .modal-body h2,
    .modal-body h3,
    .modal-body h4 {
      color: var(--text);
      margin: 18px 0 8px 0;
    }

    .modal-body ul {
      padding-left: 18px;
      margin: 8px 0;
    }

    .modal-body li {
      margin: 6px 0;
    }

    .card-body h2,
    .card-body h3,
    .card-body h4 {
      color: var(--text);
      margin: 18px 0 8px 0;
    }

    .card-body ul {
      padding-left: 18px;
      margin: 8px 0;
    }

    .card-body li {
      margin: 6px 0;
    }

    /* 链接样式 */
    .card-body a,
    .modal-body a {
      color: var(--accent);
      text-decoration: none;
      border-bottom: 1px solid rgba(77, 211, 255, 0.4);
      transition: all 0.2s ease;
    }

    .card-body a:hover,
    .modal-body a:hover {
      color: var(--accent-strong);
      border-bottom-color: var(--accent-strong);
      background: rgba(77, 211, 255, 0.1);
      padding: 2px 4px;
      margin: -2px -4px;
      border-radius: 4px;
    }

    .tag {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 4px 10px;
      border-radius: 999px;
      background: rgba(77, 211, 255, 0.12);
      color: var(--accent);
      font-size: 12px;
      border: 1px solid rgba(77, 211, 255, 0.3);
    }

    .divider {
      height: 1px;
      background: var(--border);
      margin: 18px 0 0 0;
    }

    @media (max-width: 960px) {
      .hero, main {
        padding: 32px 24px 40px 24px;
      }

      .card {
        padding: 20px;
      }
    }
  </style>
</head>
<body>
  <section class="hero">
    <h1>IntelliAI Platform Release Notes</h1>
    <p>The IntelliAI Engine powers all IntelliAI plugins. This page aggregates changes from each module so you can scan updates at a glance.</p>
    <div class="meta">
      <span class="tag">Aggregated Changelog</span>
      <span>Generated at: __GENERATED_AT__</span>
    </div>
  </section>
  <main>
HTML

  # 遍历每个插件目录，生成对应的卡片内容
  card_index=0
  while IFS= read -r plugin_dir; do
    # 插件配置文件路径
    plugin_xml="$plugin_dir/src/main/resources/META-INF/plugin.xml"

    # 从 plugin.xml 中提取插件名称（使用 ripgrep 查找 <name> 标签）
    plugin_name=$(rg -n "<name>" "$plugin_xml" | head -n 1 | sed -E 's/.*<name>([^<]+).*/\1/')
    # 如果提取失败，使用目录名作为插件名称
    if [[ -z "$plugin_name" ]]; then
      plugin_name="$(basename "$plugin_dir")"
    fi

    # 模块名称（使用目录名）
    module_name="$(basename "$plugin_dir")"
    # 更新日志文件路径
    changes_file="$plugin_dir/includes/pluginChanges.html"

    # 读取更新日志内容
    full_content=$(cat "$changes_file")

    # 提取预览内容：提取第一个版本块中的前 5 个列表项
    # 策略：提取第一个 <h3> 版本块，然后在这个块中提取前 5 个 <li> 项

    # 找到第一个和第二个 <h3> 标签的行号
    first_h3_line=$(echo "$full_content" | grep -n "<h3>" | head -n 1 | cut -d: -f1)
    second_h3_line=$(echo "$full_content" | grep -n "<h3>" | sed -n '2p' | cut -d: -f1)

    # 提取第一个版本块
    if [[ -n "$first_h3_line" ]]; then
      if [[ -n "$second_h3_line" ]]; then
        # 提取从第一个 <h3> 到第二个 <h3> 之前的内容
        first_version_block=$(echo "$full_content" | sed -n "${first_h3_line},$((second_h3_line - 1))p")
      else
        # 如果没有第二个 <h3>，提取从第一个 <h3> 到文件结束
        first_version_block=$(echo "$full_content" | sed -n "${first_h3_line},\$p")
      fi
    else
      # 如果没有找到 <h3>，使用前 12 行
      first_version_block=$(echo "$full_content" | head -n 12)
    fi

    # 从第一个版本块中提取前 5 个 <li> 项
    # 使用更简单的方法：提取包含 <li> 的行，直到找到 5 个完整的 <li> 项
    preview_content=$(echo "$first_version_block" | awk '
      BEGIN {
        li_count = 0;
        in_li = 0;
        output = "";
      }
      # 输出 <h3> 和 <ul> 标签
      /<h3>/ || /<ul>/ {
        output = output $0 "\n";
        next;
      }
      # 开始一个 <li> 项
      /<li>/ {
        if (li_count < 5) {
          in_li = 1;
          li_count++;
          output = output $0 "\n";
          # 如果同一行就闭合了，标记为不在 <li> 中
          if (match($0, /<\/li>/)) {
            in_li = 0;
          }
          next;
        }
      }
      # 结束一个 <li> 项
      /<\/li>/ {
        if (in_li) {
          output = output $0 "\n";
          in_li = 0;
        }
        next;
      }
      # <li> 中间的内容
      {
        if (in_li && li_count <= 5) {
          output = output $0 "\n";
        }
      }
      END {
        # 如果输出中有 <ul> 但没有 </ul>，添加闭合标签
        if (match(output, /<ul>/) && !match(output, /<\/ul>/)) {
          output = output "</ul>\n";
        }
        print output;
      }
    ')

    # 如果提取的内容为空或太少，则使用前 10 行作为预览
    if [[ -z "$preview_content" ]] || [[ $(echo "$preview_content" | grep -c "<li>") -lt 2 ]]; then
      preview_content=$(echo "$full_content" | head -n 10)
    fi

    # 检查是否有更多内容
    full_li_count=$(echo "$full_content" | grep -c "<li>")
    preview_li_count=$(echo "$preview_content" | grep -c "<li>")
    # 如果原始内容有超过 5 个 <li> 项，认为有更多内容
    has_more=$((full_li_count > 5 ? 1 : 0))

    # 获取插件链接
    home_url=$(get_plugin_home_url "$module_name")
    docs_url=$(get_plugin_docs_url "$module_name")
    download_url=$(get_plugin_download_url "$module_name")

    # 输出插件卡片 HTML 结构（只显示预览内容）
    echo "    <section class=\"card\" onclick=\"openModal($card_index)\">"
    echo "      <div class=\"card-header\">"
    echo "        <div class=\"card-header-left\">"
    echo "          <div class=\"card-title\">$plugin_name</div>"
    echo "          <div class=\"card-meta\">$module_name</div>"
    echo "        </div>"
      echo "        <div class=\"card-header-actions\" onclick=\"event.stopPropagation()\">"
      if [[ -n "$home_url" ]]; then
        echo "          <a href=\"#\" onclick=\"openLink('$home_url'); return false;\" class=\"card-button card-button-home\">🏠 主页</a>"
      fi
      if [[ -n "$docs_url" ]]; then
        echo "          <a href=\"#\" onclick=\"openLink('$docs_url'); return false;\" class=\"card-button card-button-docs\">📚 文档</a>"
      fi
      if [[ -n "$download_url" ]]; then
        echo "          <a href=\"#\" onclick=\"openLink('$download_url'); return false;\" class=\"card-button card-button-download\" title=\"因插件市场审核延迟, 可下载最新版本手动安装\">⬇️ 离线安装</a>"
      fi
      echo "        </div>"
    echo "      </div>"
    echo "      <div class=\"card-body\" id=\"preview-$card_index\">"
    echo "$preview_content"
    echo "      </div>"
    if [[ $has_more -eq 1 ]]; then
      echo "      <div class=\"card-footer\">点击查看完整更新日志 →</div>"
    fi
    echo "    </section>"

    # 输出完整内容到隐藏的模态框数据中
    echo "    <script type=\"text/template\" id=\"modal-content-$card_index\" style=\"display:none;\">"
    echo "$full_content"
    echo "    </script>"

    card_index=$((card_index + 1))
  done <<< "$sorted_dirs"

  # 输出 HTML 页面的结束部分和 JavaScript
  cat <<'HTML'
  </main>

  <!-- 模态框 -->
  <div id="modal" class="modal" onclick="closeModalOnBackdrop(event)">
    <div class="modal-content" onclick="event.stopPropagation()">
      <div class="modal-header">
        <h2 class="modal-title" id="modal-title"></h2>
        <button class="modal-close" onclick="closeModal()">&times;</button>
      </div>
      <div class="modal-body" id="modal-body"></div>
    </div>
  </div>

  <script>
    // 打开链接（兼容 JCEF 浏览器）
    function openLink(url) {
      if (url) {
        window.open(url, '_blank', 'noopener,noreferrer');
      }
    }

    // 打开模态框
    function openModal(index) {
      const modal = document.getElementById('modal');
      const modalTitle = document.getElementById('modal-title');
      const modalBody = document.getElementById('modal-body');
      const template = document.getElementById('modal-content-' + index);

      if (!template) return;

      // 从卡片标题获取插件名称
      const card = document.querySelectorAll('.card')[index];
      const cardTitle = card.querySelector('.card-title').textContent;

      modalTitle.textContent = cardTitle;
      modalBody.innerHTML = template.textContent;
      modal.classList.add('active');

      // 阻止背景滚动
      document.body.style.overflow = 'hidden';
    }

    // 关闭模态框
    function closeModal() {
      const modal = document.getElementById('modal');
      modal.classList.remove('active');
      document.body.style.overflow = '';
    }

    // 点击背景关闭模态框
    function closeModalOnBackdrop(event) {
      if (event.target.id === 'modal') {
        closeModal();
      }
    }

    // ESC 键关闭模态框
    document.addEventListener('keydown', function(event) {
      if (event.key === 'Escape') {
        closeModal();
      }
    });
  </script>
</body>
</html>
HTML
} | sed "s/__GENERATED_AT__/$generated_at/" > "$OUTPUT_FILE"
# 将占位符 __GENERATED_AT__ 替换为实际的生成时间

# 输出生成成功信息
echo "Generated: $OUTPUT_FILE"

# 拷贝一份到 intelli-ai-engine/src/main/resources/whatsnew/ 目录
mkdir -p "$TARGET_DIR"
cp "$OUTPUT_FILE" "$TARGET_DIR/latest.html"
echo "Copied to: $TARGET_DIR/latest.html"

# 解析 Engine 插件的最新版本并创建版本文件
ENGINE_CHANGES_FILE="$ENGINE_DIR/includes/pluginChanges.html"
if [ -f "$ENGINE_CHANGES_FILE" ]; then
    echo ""
    echo "解析 Engine 插件最新版本..."

    # 提取第一个 <h3> 标签中的版本号（去除 HTML 标签）
    LATEST_VERSION=$(grep -m 1 "<h3>" "$ENGINE_CHANGES_FILE" | sed 's/<[^>]*>//g' | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')

    if [ -n "$LATEST_VERSION" ]; then
        echo "  最新版本: $LATEST_VERSION"

        # 将版本号转换为文件名格式（如 2025.3.1 -> 2025_3_1）
        VERSION_FILE_NAME=$(echo "$LATEST_VERSION" | sed 's/\./_/g')
        VERSION_HTML_FILE="$TARGET_DIR/${VERSION_FILE_NAME}.html"

        # 提取第一个版本块的内容（从第一个版本号到下一个不同版本号之前）
        # 使用兼容 BSD awk 和 GNU awk 的方法提取包含中英文两个版本块的完整内容
        awk -v latest_version="$LATEST_VERSION" '
            BEGIN {
                in_version_block = 0
                version_started = 0
            }
            /<h3>/ {
                # 提取当前行的版本号（兼容 BSD awk）
                if (match($0, /<h3>[^<]+<\/h3>/)) {
                    # 提取匹配的版本号部分
                    matched = substr($0, RSTART, RLENGTH)
                    # 移除 HTML 标签
                    gsub(/<[^>]+>/, "", matched)
                    current_version = matched
                    gsub(/^[[:space:]]+|[[:space:]]+$/, "", current_version)

                    if (!version_started) {
                        # 找到第一个版本号，开始提取
                        if (current_version == latest_version) {
                            in_version_block = 1
                            version_started = 1
                            print $0
                            next
                        }
                    } else {
                        # 如果已经开始了，检查是否是下一个不同版本号
                        if (current_version != latest_version) {
                            # 遇到下一个不同版本号，停止提取
                            exit
                        }
                    }
                }
            }
            {
                if (in_version_block) {
                    print $0
                }
            }
        ' "$ENGINE_CHANGES_FILE" > "$VERSION_HTML_FILE"

        if [ -s "$VERSION_HTML_FILE" ]; then
            echo "  覆盖版本文件: $VERSION_HTML_FILE"
        else
            echo "  警告: 版本文件内容为空，可能提取失败"
        fi

        # 更新 InternalWhatsNewProvider.java 文件
        PROVIDER_JAVA_FILE="$ENGINE_DIR/src/main/java/dev/dong4j/zeka/stack/idea/plugin/common/whatsnew/InternalWhatsNewProvider.java"
        if [ -f "$PROVIDER_JAVA_FILE" ]; then
            echo "  更新 InternalWhatsNewProvider.java..."

            # 检查是否已存在该版本的映射
            if grep -q "\"$LATEST_VERSION\"" "$PROVIDER_JAVA_FILE"; then
                echo "    版本 $LATEST_VERSION 已存在，跳过添加"
            else
                # 创建临时文件，在 // version mark 注释后添加新的映射
                TEMP_FILE=$(mktemp)
                awk -v version="$LATEST_VERSION" -v filename="${VERSION_FILE_NAME}.html" '
                    /\/\/ version mark/ {
                        print $0
                        print "        new DefaultWhatsNewPage(\"" version "\", \"" filename "\"),"
                        next
                    }
                    { print }
                ' "$PROVIDER_JAVA_FILE" > "$TEMP_FILE"
                mv "$TEMP_FILE" "$PROVIDER_JAVA_FILE"
                echo "    已添加版本映射: $LATEST_VERSION -> ${VERSION_FILE_NAME}.html"
            fi
        else
            echo "  警告: 找不到 InternalWhatsNewProvider.java 文件: $PROVIDER_JAVA_FILE"
        fi
    else
        echo "  警告: 无法解析最新版本号"
    fi
else
    echo "  警告: 找不到 Engine 插件更新日志文件: $ENGINE_CHANGES_FILE"
fi

