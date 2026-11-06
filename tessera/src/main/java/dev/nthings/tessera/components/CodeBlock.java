package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.code;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.pre;
import static dev.nthings.tessera.core.Elements.span;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.RenderView;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.foundation.FluentBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/// Accessible keyboard-scrollable code block backed by CSS counter line numbers
/// (`counter-increment: code-line`), line highlighting, diff annotations, and optional header bars.
public final class CodeBlock implements RenderView, FluentBuilder<CodeBlock> {

  private final List<String> lines = new ArrayList<>();
  private final Set<Integer> highlightedLines = new LinkedHashSet<>();
  private final Set<Integer> addedLines = new LinkedHashSet<>();
  private final Set<Integer> deletedLines = new LinkedHashSet<>();
  private @Nullable String filename;
  private @Nullable String language;
  private boolean diffAutoDetect = false;

  private CodeBlock(List<String> lines) {
    this.lines.addAll(lines);
  }

  public static CodeBlock of(String code) {
    var normalized = code.replace("\r\n", "\n").replace('\r', '\n');
    return new CodeBlock(Arrays.asList(normalized.split("\n", -1)));
  }

  public static CodeBlock ofLines(String... lines) {
    return new CodeBlock(Arrays.asList(lines));
  }

  public static CodeBlock ofLines(List<String> lines) {
    return new CodeBlock(lines);
  }

  @Override
  public CodeBlock self() {
    return this;
  }

  public CodeBlock filename(String filename) {
    this.filename = filename;
    return this;
  }

  public CodeBlock language(String language) {
    this.language = language;
    return this;
  }

  public CodeBlock highlightLines(int... lineNumbers) {
    for (var line : lineNumbers) {
      this.highlightedLines.add(line);
    }
    return this;
  }

  public CodeBlock addedLines(int... lineNumbers) {
    for (var line : lineNumbers) {
      this.addedLines.add(line);
    }
    return this;
  }

  public CodeBlock deletedLines(int... lineNumbers) {
    for (var line : lineNumbers) {
      this.deletedLines.add(line);
    }
    return this;
  }

  public CodeBlock diffAutoDetect(boolean enabled) {
    this.diffAutoDetect = enabled;
    return this;
  }

  @Override
  public String viewType() {
    return "tessera-code-block";
  }

  @Override
  public void configureScope(ScopeStyle scope) {
    scope
        .root(
            s ->
                s.my(Tokens.SPACE_5)
                    .rounded(Tokens.RADIUS_LG)
                    .borderSubtle(Tokens.BORDER_SUBTLE)
                    .bg(Tokens.SURFACE_CARD)
                    .overflowClip())
        .select(
            ":scope > .code-header",
            s ->
                s.flex()
                    .justifyBetween()
                    .itemsCenter()
                    .px(Tokens.SPACE_4)
                    .py(CssLength.rem(0.45))
                    .bg(Tokens.SURFACE_SUBTLE)
                    .borderBlockEndSubtle(Tokens.BORDER_SUBTLE)
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
                    .lineHeight("1.6"))
        .select(":scope > .code-pre > .code-lines", s -> s.block())
        .select(
            ":scope > .code-pre > .code-lines > .code-line",
            s -> s.block().counterIncrement("code-line").whiteSpace("pre").px(Tokens.SPACE_4))
        .select(
            ":scope > .code-pre > .code-lines > .code-line::before",
            s ->
                s.content("counter(code-line)")
                    .inlineFlex()
                    .justifyEnd()
                    .inlineSize(CssLength.rem(2))
                    .me(Tokens.SPACE_5)
                    .textColor(Tokens.TEXT_MUTED)
                    .opacity(0.6))
        .select(
            ":scope > .code-pre > .code-lines > .code-line.is-highlighted",
            s -> s.bgRaw(Tokens.ACCENT_PRIMARY.mixTransparent(15)))
        .select(
            ":scope > .code-pre > .code-lines > .code-line.is-add",
            s -> s.bgRaw(Tokens.STATUS_SUCCESS.mixTransparent(16)).textColor(Tokens.STATUS_SUCCESS))
        .select(
            ":scope > .code-pre > .code-lines > .code-line.is-del",
            s -> s.bgRaw(Tokens.STATUS_DANGER.mixTransparent(16)).textColor(Tokens.STATUS_DANGER));
  }

  @Override
  public Element render() {
    var wrapper = div().attr("class", "code-block-wrapper");
    if (filename != null || language != null) {
      var header = div().attr("class", "code-header");
      if (filename != null) {
        header.child(span().attr("class", "code-filename").text(filename));
      } else {
        header.child(span());
      }
      if (language != null) {
        header.child(Pill.neutral(language).sm());
      }
      wrapper.child(header);
    }

    var codeEl = code().attr("class", "code-lines");

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

      var lineSpan = span().attr("class", "code-line");
      if (isAdd) {
        lineSpan.attr("class", "code-line is-add");
      } else if (isDel) {
        lineSpan.attr("class", "code-line is-del");
      } else if (isHighlight) {
        lineSpan.attr("class", "code-line is-highlighted");
      }

      lineSpan.text(content);
      codeEl.child(lineSpan);
    }

    var preEl =
        pre()
            .attr("class", "code-pre")
            .tabIndex(0)
            .refine(
                s ->
                    s.fontFamily(Tokens.FONT_MONO.varReference())
                        .overflowXAuto()
                        .counterReset("code-line"))
            .child(codeEl);

    // CSS counter increment on lines
    return wrapper.child(preEl);
  }
}
