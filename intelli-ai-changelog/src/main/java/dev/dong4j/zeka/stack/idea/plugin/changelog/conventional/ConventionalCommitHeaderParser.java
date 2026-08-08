package dev.dong4j.zeka.stack.idea.plugin.changelog.conventional;

import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解析 Git 提交消息文档中的 Conventional Commit 首行。
 * <p>
 * Conventional Commits 规定 header 独占第一行，body 从第二个空行段落开始；
 * 本解析器只读取到第一个 {@code \n}（或 {@code \r\n}）为止，避免把 body 或列表项误判为 header 的一部分。
 *
 * @author dong4j
 * @since 1.0.0
 */
public final class ConventionalCommitHeaderParser {

    /**
     * 首行结构：type、可选 scope（含未闭合括号）、可选 {@code !}、可选 {@code :}、可选 subject 余下部分。
     */
    private static final Pattern FIRST_LINE_PATTERN = Pattern.compile(
        "^(?<type>[^\\s(:!]+)(?<scope>\\([^)\\r\\n]*\\)?)?(?<breaking>!)?(?<sep>:)?(?<subject>.*)?$");

    private ConventionalCommitHeaderParser() {
        throw new UnsupportedOperationException("Utility class");
    }

    @NotNull
    public static ConventionalCommitHeader parseFirstLine(@NotNull CharSequence documentText) {
        int firstLineEndExclusive = indexOfFirstLineBreak(documentText);
        CharSequence firstLineSeq = firstLineEndExclusive < 0
                                    ? documentText
                                    : documentText.subSequence(0, firstLineEndExclusive);
        String firstLine = firstLineSeq.toString();

        Matcher matcher = FIRST_LINE_PATTERN.matcher(firstLine);
        if (!matcher.matches()) {
            return emptyHeader(firstLine, firstLineEndExclusive(documentText, firstLineEndExclusive));
        }

        String type = groupOrNull(matcher, "type");
        TextRange typeRange = type != null ? TextRange.create(matcher.start("type"), matcher.end("type")) : null;

        String scopeRaw = groupOrNull(matcher, "scope");
        String scope = null;
        TextRange scopeRange = null;
        if (scopeRaw != null && !scopeRaw.isEmpty()) {
            scopeRange = TextRange.create(matcher.start("scope"), matcher.end("scope"));
            scope = unwrapScope(scopeRaw);
        }

        boolean hasBreaking = matcher.group("breaking") != null;
        TextRange breakingRange = hasBreaking
                                  ? TextRange.create(matcher.start("breaking"), matcher.end("breaking"))
                                  : null;

        boolean hasSeparator = matcher.group("sep") != null;
        TextRange separatorRange = hasSeparator
                                   ? TextRange.create(matcher.start("sep"), matcher.end("sep"))
                                   : null;

        String subject = groupOrNull(matcher, "subject");
        TextRange subjectRange = null;
        if (matcher.start("subject") <= matcher.end("subject") && hasSeparator) {
            subjectRange = TextRange.create(matcher.start("subject"), matcher.end("subject"));
        } else if (subject != null && !subject.isEmpty() && hasSeparator) {
            subjectRange = TextRange.create(matcher.start("subject"), matcher.end("subject"));
        }

        return new ConventionalCommitHeader(
            firstLine,
            firstLineEndExclusive(documentText, firstLineEndExclusive),
            type,
            scope,
            subject,
            hasBreaking,
            hasSeparator,
            typeRange,
            scopeRange,
            breakingRange,
            separatorRange,
            subjectRange
        );
    }

    @NotNull
    public static ConventionalCommitContext contextAt(
        @NotNull ConventionalCommitHeader header,
        int offsetInFirstLine) {
        if (offsetInFirstLine < 0 || offsetInFirstLine > header.firstLine().length()) {
            return ConventionalCommitContext.OTHER;
        }

        TextRange typeRange = header.typeRange();
        if (typeRange != null) {
            if (isInTypeContext(header, offsetInFirstLine, typeRange)) {
                return ConventionalCommitContext.TYPE;
            }
        } else if (offsetInFirstLine == 0) {
            // 首行为空（或无法解析出 type，如全空白）时，行首光标视为待输入 type，
            // 用于触发 type 补全；避免下方 isBreakingInsertPosition 的 0 兜底值把它误判为 BREAKING。
            return ConventionalCommitContext.TYPE;
        }

        TextRange scopeRange = header.scopeRange();
        if (scopeRange != null && scopeRange.contains(offsetInFirstLine)) {
            return ConventionalCommitContext.SCOPE;
        }

        TextRange breakingRange = header.breakingRange();
        if (breakingRange != null && breakingRange.contains(offsetInFirstLine)) {
            return ConventionalCommitContext.BREAKING;
        }

        if (isBreakingInsertPosition(header, offsetInFirstLine)) {
            return ConventionalCommitContext.BREAKING;
        }

        TextRange subjectRange = header.subjectRange();
        if (subjectRange != null && subjectRange.contains(offsetInFirstLine)) {
            return ConventionalCommitContext.SUBJECT;
        }

        TextRange separatorRange = header.separatorRange();
        if (separatorRange != null && separatorRange.contains(offsetInFirstLine)) {
            return ConventionalCommitContext.OTHER;
        }

        return ConventionalCommitContext.OTHER;
    }

    private static boolean isInTypeContext(
        @NotNull ConventionalCommitHeader header,
        int offset,
        @NotNull TextRange typeRange) {
        if (typeRange.contains(offset)) {
            return true;
        }
        // 光标停在 type 末尾且尚未进入 scope / breaking / separator 时仍视为 TYPE（如 "fe|"）
        return offset == typeRange.getEndOffset()
               && !header.hasSeparator()
               && header.scopeRange() == null
               && !header.hasBreakingChange();
    }

    /**
     * type/scope 之后、{@code :} 之前且无 {@code !} 时，该位置可插入 breaking marker。
     */
    private static boolean isBreakingInsertPosition(
        @NotNull ConventionalCommitHeader header,
        int offset) {
        // breaking marker 只能出现在已解析出 type 之后；空 header（typeRange == null）时
        // endOfTypeAndScope 会回退返回 0，若不做该判断，offset==0 会被误判为 BREAKING。
        if (header.hasBreakingChange() || header.hasSeparator() || header.typeRange() == null) {
            return false;
        }
        int afterStructure = endOfTypeAndScope(header);
        return offset == afterStructure;
    }

    private static int endOfTypeAndScope(@NotNull ConventionalCommitHeader header) {
        if (header.scopeRange() != null) {
            return header.scopeRange().getEndOffset();
        }
        if (header.typeRange() != null) {
            return header.typeRange().getEndOffset();
        }
        return 0;
    }

    private static int indexOfFirstLineBreak(@NotNull CharSequence text) {
        int len = text.length();
        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                return i;
            }
            if (c == '\r') {
                return i;
            }
        }
        return -1;
    }

    private static int firstLineEndExclusive(@NotNull CharSequence documentText, int lineBreakIndex) {
        return lineBreakIndex < 0 ? documentText.length() : lineBreakIndex;
    }

    @NotNull
    private static ConventionalCommitHeader emptyHeader(@NotNull String firstLine, int firstLineEndExclusive) {
        return new ConventionalCommitHeader(
            firstLine,
            firstLineEndExclusive,
            null,
            null,
            null,
            false,
            false,
            null,
            null,
            null,
            null,
            null
        );
    }

    @org.jetbrains.annotations.Nullable
    private static String groupOrNull(@NotNull Matcher matcher, @NotNull String name) {
        String g = matcher.group(name);
        return g == null || g.isEmpty() ? null : g;
    }

    @NotNull
    private static String unwrapScope(@NotNull String scopeWithParens) {
        if (scopeWithParens.startsWith("(")) {
            String inner = scopeWithParens.substring(1);
            if (inner.endsWith(")")) {
                inner = inner.substring(0, inner.length() - 1);
            }
            return inner;
        }
        return scopeWithParens;
    }
}
