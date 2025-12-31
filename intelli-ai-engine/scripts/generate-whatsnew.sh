#!/usr/bin/env bash
# 生成 IntelliAI 平台的聚合更新日志页面
# 
# 功能说明：
# 1. 扫描整个项目，查找所有插件的 pluginChanges.html 文件
# 2. 验证每个文件对应的目录是否为有效的插件目录（包含 plugin.xml）
# 3. 从 plugin.xml 中提取插件名称
# 4. 生成一个聚合的 HTML 页面，包含所有插件的更新日志
# 5. 生成的页面使用现代化的深色主题样式，每个插件以卡片形式展示
#
# 输出文件：intelli-ai-engine/src/main/resources/whatsnew/index.html
# 该文件用于在 IDE 中显示 "What's New" 对话框

set -euo pipefail

# 获取脚本所在目录和项目根目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
ENGINE_DIR="$ROOT_DIR/intelli-ai-engine"
OUTPUT_FILE="$ENGINE_DIR/src/main/resources/whatsnew/index.html"

# 忽略列表：不包含在 What's New 页面中的插件目录名
# 可以添加需要忽略的插件目录名，用空格分隔
IGNORE_PLUGINS=(
  "idea-plugin-template"
  "reference"
  "feedback-server"
  "archiver-man"
  "intelli-ai-agent-template"
)

# 检查插件是否在忽略列表中
is_ignored() {
  local plugin_dir="$1"
  local plugin_name="$(basename "$plugin_dir")"
  for ignored in "${IGNORE_PLUGINS[@]}"; do
    if [[ "$plugin_name" == "$ignored" ]]; then
      return 0  # 在忽略列表中
    fi
  done
  return 1  # 不在忽略列表中
}

# 查找所有插件的 pluginChanges.html 文件
# 这些文件通常位于：{插件目录}/includes/pluginChanges.html
mapfile -d '' CHANGE_FILES < <(find "$ROOT_DIR" -type f -path "*/includes/pluginChanges.html" -print0)

# 验证每个文件对应的目录是否为有效的插件目录
# 通过检查是否存在 plugin.xml 文件来确认，并排除忽略列表中的插件
plugin_dirs=()
for change_file in "${CHANGE_FILES[@]}"; do
  # 获取插件目录：pluginChanges.html 位于 includes/ 目录下，需要向上两级
  plugin_dir="$(dirname "$(dirname "$change_file")")"
  # 验证是否为有效的插件目录（包含 plugin.xml）且不在忽略列表中
  if [[ -f "$plugin_dir/src/main/resources/META-INF/plugin.xml" ]] && ! is_ignored "$plugin_dir"; then
    plugin_dirs+=("$plugin_dir")
  fi
done

# 如果没有找到任何插件目录，退出并报错
if [[ ${#plugin_dirs[@]} -eq 0 ]]; then
  echo "No pluginChanges.html files found." >&2
  exit 1
fi

# 对插件目录进行排序并去重
sorted_dirs=$(printf "%s\n" "${plugin_dirs[@]}" | sort -u)

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
      align-items: baseline;
      justify-content: space-between;
      gap: 12px;
      margin-bottom: 16px;
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

    # 输出插件卡片 HTML 结构（只显示预览内容）
    echo "    <section class=\"card\" onclick=\"openModal($card_index)\">"
    echo "      <div class=\"card-header\">"
    echo "        <div class=\"card-title\">$plugin_name</div>"
    echo "        <div class=\"card-meta\">$module_name</div>"
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
