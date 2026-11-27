package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.code;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.pre;
import static dev.nthings.tessera.core.Elements.span;
import static dev.nthings.tessera.core.Elements.text;

import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.core.ViewContext;
import dev.nthings.tessera.css.Border;
import dev.nthings.tessera.css.Counter;
import dev.nthings.tessera.css.CssColor;
import dev.nthings.tessera.css.CssContent;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.DesignToken;
import dev.nthings.tessera.css.Overflow;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.Tone;
import dev.nthings.tessera.css.WhiteSpace;
import dev.nthings.tessera.internal.components.Syntax;
import dev.nthings.tessera.internal.css.Tint;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/// Source listing in a keyboard-focusable `<pre>`, one `<span class="code-line">` per line. It can
/// show a filename and a language in a header, color the source's syntax, and mark lines as
/// highlighted, added, or deleted. Line numbers are display-only and are not part of the copied
/// text. Create one with [#of(String)] for a source string or [#ofLines(String...)] for lines you
/// already have.
///
/// Without an [#scopeId(String)], the compiler numbers instances `tessera-code-block-<n>`.
public final class CodeBlock extends BuiltinScope<CodeBlock> {

  private static final Counter LINE_COUNTER = Counter.named("code-line");

  private final List<String> lines;
  private final Set<Integer> highlightedLines = new LinkedHashSet<>();
  private final Set<Integer> addedLines = new LinkedHashSet<>();
  private final Set<Integer> deletedLines = new LinkedHashSet<>();
  private @Nullable String filename;
  private @Nullable String language;
  private boolean diffAutoDetect = false;

  private CodeBlock(List<String> lines) {
    this.lines = lines;
  }

  /// Creates a block from `code`, splitting lines on `\n`, `\r\n`, or `\r`. A trailing newline adds
  /// an empty last line.
  public static CodeBlock of(String code) {
    var normalized = code.replace("\r\n", "\n").replace('\r', '\n');
    return new CodeBlock(List.of(normalized.split("\n", -1)));
  }

  /// Creates a block with one line per element. An element is not split, so an embedded newline
  /// stays on one numbered line.
  public static CodeBlock ofLines(String... lines) {
    return new CodeBlock(List.of(lines));
  }

  @Override
  public CodeBlock self() {
    return this;
  }

  /// Shows `filename` in the header above the code.
  public CodeBlock filename(String filename) {
    this.filename = filename;
    return this;
  }

  /// Shows `language` as a small pill in the header and colors the source's syntax when the
  /// language is one of `java`, `javascript` (`js`, `jsx`), `typescript` (`ts`, `tsx`), `json`,
  /// `css`, `html` (`xml`, `svg`), `shell` (`sh`, `bash`, `zsh`), `yaml` (`yml`), `python` (`py`)
  /// or `sql`, ignoring case. Each colored span is a `<span class="tok-<kind>">` painted with the
  /// matching `Tokens.SYNTAX_*` color; added and deleted lines keep their own text color.
  public CodeBlock language(String language) {
    this.language = language;
    return this;
  }

  /// Highlights the given 1-based line numbers; numbers past the last line have no effect. A line
  /// that is also added or deleted shows that state, with added before deleted before highlighted.
  public CodeBlock highlightLines(int... lineNumbers) {
    for (var line : lineNumbers) {
      this.highlightedLines.add(line);
    }
    return this;
  }

  /// Marks the given 1-based line numbers as added.
  public CodeBlock addedLines(int... lineNumbers) {
    for (var line : lineNumbers) {
      this.addedLines.add(line);
    }
    return this;
  }

  /// Marks the given 1-based line numbers as deleted.
  public CodeBlock deletedLines(int... lineNumbers) {
    for (var line : lineNumbers) {
      this.deletedLines.add(line);
    }
    return this;
  }

  /// Marks a line starting with `+` as added and one starting with `-` as deleted. The marker stays
  /// in the text. Explicit line numbers still apply.
  public CodeBlock diffAutoDetect() {
    this.diffAutoDetect = true;
    return this;
  }

  @Override
  String scopeName() {
    return "tessera-code-block";
  }

  @Override
  void configureScope(ScopeStyle scope) {
    scope
        .root(s ->
            s.my(Tokens.SPACE_5)
                .rounded(Tokens.RADIUS_LG)
                .border(Border.hairline(Tokens.BORDER_SUBTLE))
                .bg(Tokens.SURFACE_CARD)
                .overflow(Overflow.CLIP))
        .select(
            ":scope > .code-header",
            s ->
                s.flex()
                    .justifyBetween()
                    .itemsCenter()
                    .px(Tokens.SPACE_4)
                    .py(CssLength.rem(0.45))
                    .bg(Tokens.SURFACE_SUBTLE)
                    .borderBlockEnd(Border.hairline(Tokens.BORDER_SUBTLE))
                    .fontSize(Tokens.FONT_SIZE_XS)
                    .textColor(Tokens.TEXT_MUTED))
        .select(
            ":scope > .code-header > .code-filename",
            s -> s.fontWeight(600).textColor(Tokens.TEXT_PRIMARY))
        .select(
            ":scope > .code-pre",
            s ->
                s.m(CssLength.px(0))
                    .py(Tokens.SPACE_3)
                    .bg(Tokens.SURFACE_CARD)
                    .fontSize(Tokens.FONT_SIZE_SM)
                    .lineHeight(1.6))
        .select(":scope > .code-pre > .code-lines", s -> s.block())
        .select(
            ":scope > .code-pre > .code-lines > .code-line",
            s ->
                s.block()
                    .counterIncrement(LINE_COUNTER)
                    .whiteSpace(WhiteSpace.PRE)
                    .px(Tokens.SPACE_4))
        .select(
            ":scope > .code-pre > .code-lines > .code-line::before",
            s ->
                s.content(CssContent.counter(LINE_COUNTER))
                    .inlineFlex()
                    .justifyEnd()
                    .inlineSize(CssLength.rem(2))
                    .me(Tokens.SPACE_5)
                    .textColor(Tokens.TEXT_MUTED)
                    .opacity(0.6))
        .select(
            ":scope > .code-pre > .code-lines > .code-line.is-highlighted",
            s -> s.bg(Tint.CODE_HIGHLIGHT.fill(Tone.ACCENT)))
        .select(
            ":scope > .code-pre > .code-lines > .code-line.is-add",
            s ->
                s.bg(Tint.CODE_CHANGE.fill(Tone.SUCCESS))
                    .textColor(Tint.CODE_CHANGE.text(Tone.SUCCESS)))
        .select(
            ":scope > .code-pre > .code-lines > .code-line.is-del",
            s ->
                s.bg(Tint.CODE_CHANGE.fill(Tone.DANGER))
                    .textColor(Tint.CODE_CHANGE.text(Tone.DANGER)));
    for (var kind : Syntax.Kind.values()) {
      scope.select(":scope ." + kind.cssClass(), s -> s.textColor(syntaxColor(kind)));
    }
    // Changed lines read in their own color, as a diff does.
    scope.select(
        ":scope > .code-pre > .code-lines > .code-line:is(.is-add, .is-del) [class^=\"tok-\"]",
        s -> s.textColor(CssColor.currentColor()));
  }

  private static DesignToken.Color syntaxColor(Syntax.Kind kind) {
    return switch (kind) {
      case COMMENT -> Tokens.SYNTAX_COMMENT;
      case KEYWORD -> Tokens.SYNTAX_KEYWORD;
      case STRING -> Tokens.SYNTAX_STRING;
      case CONSTANT -> Tokens.SYNTAX_CONSTANT;
      case TYPE -> Tokens.SYNTAX_TYPE;
      case FUNCTION -> Tokens.SYNTAX_FUNCTION;
      case PROPERTY -> Tokens.SYNTAX_PROPERTY;
      case TAG -> Tokens.SYNTAX_TAG;
      case META -> Tokens.SYNTAX_META;
    };
  }

  @Override
  TagElement expand(ViewContext context) {
    var wrapper = div().attr("class", "code-block-wrapper");
    if (filename != null || language != null) {
      var header = div().attr("class", "code-header");
      if (filename != null) {
        header.child(span().attr("class", "code-filename").text(filename));
      } else {
        header.child(span());
      }
      if (language != null) {
        header.child(Badge.of(language).pill().size(Size.SM));
      }
      wrapper.child(header);
    }

    var codeEl = code().attr("class", "code-lines");
    var spans =
        language == null
            ? List.<Syntax.Span>of()
            : Syntax.highlight(language, String.join("\n", lines)).orElse(List.of());
    var lineStart = 0;
    var next = 0;

    for (var i = 0; i < lines.size(); i++) {
      var lineNum = i + 1;
      var rawLine = lines.get(i);
      var content = rawLine.isEmpty() ? " " : rawLine;

      var isAdd = addedLines.contains(lineNum);
      var isDel = deletedLines.contains(lineNum);
      if (diffAutoDetect) {
        if (rawLine.startsWith("+")) {
          isAdd = true;
        } else if (rawLine.startsWith("-")) {
          isDel = true;
        }
      }
      var isHighlight = highlightedLines.contains(lineNum);

      var lineClass =
          isAdd
              ? "code-line is-add"
              : isDel ? "code-line is-del" : isHighlight ? "code-line is-highlighted" : "code-line";
      var line = span().attr("class", lineClass);
      var lineEnd = lineStart + rawLine.length();
      if (rawLine.isEmpty()) {
        line.text(content);
      } else {
        // Spans that cross a line break are split into one piece per line.
        var at = lineStart;
        while (next < spans.size() && spans.get(next).start() < lineEnd) {
          var token = spans.get(next);
          var from = Math.max(token.start(), lineStart);
          var to = Math.min(token.end(), lineEnd);
          if (from > at) {
            line.child(text(rawLine.substring(at - lineStart, from - lineStart)));
          }
          if (to > from) {
            line.child(
                span()
                    .attr("class", token.kind().cssClass())
                    .text(rawLine.substring(from - lineStart, to - lineStart)));
          }
          at = Math.max(at, to);
          if (token.end() > lineEnd) {
            break;
          }
          next++;
        }
        if (at < lineEnd) {
          line.child(text(rawLine.substring(at - lineStart)));
        }
      }
      codeEl.child(line);
      lineStart = lineEnd + 1;
    }

    var preEl =
        pre()
            .attr("class", "code-pre")
            .tabIndex(0)
            .styles(s ->
                s.fontFamily(Tokens.FONT_MONO).overflowX(Overflow.AUTO).counterReset(LINE_COUNTER))
            .child(codeEl);

    return wrapper.child(preEl);
  }
}
