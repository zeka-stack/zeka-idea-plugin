package dev.dong4j.zeka.stack.idea.plugin.changelog.conventional;

import com.intellij.openapi.util.TextRange;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConventionalCommitHeaderParserTest {

    @Test
    void shouldParseFullHeader() {
        ConventionalCommitHeader header =
            ConventionalCommitHeaderParser.parseFirstLine("feat(changelog)!: add highlight\n\n- body");

        assertThat(header.type()).isEqualTo("feat");
        assertThat(header.scope()).isEqualTo("changelog");
        assertThat(header.hasBreakingChange()).isTrue();
        assertThat(header.subject()).isEqualTo(" add highlight");
        assertThat(header.typeRange()).isEqualTo(TextRange.create(0, 4));
    }

    @Test
    void shouldParseTypeOnlyPartialInput() {
        ConventionalCommitHeader header = ConventionalCommitHeaderParser.parseFirstLine("fe");
        assertThat(header.type()).isEqualTo("fe");
        assertThat(header.scope()).isNull();
        assertThat(header.hasSeparator()).isFalse();
        assertThat(ConventionalCommitHeaderParser.contextAt(header, 2))
            .isEqualTo(ConventionalCommitContext.TYPE);
    }

    @Test
    void shouldDetectScopeContextInsideParens() {
        ConventionalCommitHeader header = ConventionalCommitHeaderParser.parseFirstLine("feat(ch");
        assertThat(ConventionalCommitHeaderParser.contextAt(header, 6))
            .isEqualTo(ConventionalCommitContext.SCOPE);
    }

    @Test
    void shouldIgnoreBodyLines() {
        ConventionalCommitHeader header =
            ConventionalCommitHeaderParser.parseFirstLine("fix: x\n\n- not header");
        assertThat(header.type()).isEqualTo("fix");
        assertThat(header.subject()).isEqualTo(" x");
    }

    @Test
    void shouldTreatSubjectAsSubjectContext() {
        ConventionalCommitHeader header =
            ConventionalCommitHeaderParser.parseFirstLine("docs: update readme");
        int offset = "docs: ".length();
        assertThat(ConventionalCommitHeaderParser.contextAt(header, offset))
            .isEqualTo(ConventionalCommitContext.SUBJECT);
    }

    @Test
    void shouldReturnTypeContextForEmptyFirstLine() {
        // 回归测试：空首行时 typeRange 为 null，endOfTypeAndScope 兜底返回 0，
        // 若 isBreakingInsertPosition 未校验 typeRange != null，会把 offset 0 误判为 BREAKING。
        ConventionalCommitHeader header = ConventionalCommitHeaderParser.parseFirstLine("");
        assertThat(header.typeRange()).isNull();
        assertThat(ConventionalCommitHeaderParser.contextAt(header, 0))
            .isEqualTo(ConventionalCommitContext.TYPE);
    }

    @Test
    void shouldParseUnknownTypeAsType() {
        // 解析器不校验 type 是否属于标准 Conventional Commit 类型集合，
        // 任意非空白/非分隔符 token 都应被解析为 type，供高亮/补全上下文识别使用。
        ConventionalCommitHeader header = ConventionalCommitHeaderParser.parseFirstLine("foo:");
        assertThat(header.type()).isEqualTo("foo");
        assertThat(header.hasSeparator()).isTrue();
        assertThat(header.typeRange()).isEqualTo(TextRange.create(0, 3));
    }
}
