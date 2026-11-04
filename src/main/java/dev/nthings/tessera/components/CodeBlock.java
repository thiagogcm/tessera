package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.code;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.pre;
import static dev.nthings.tessera.core.Elements.span;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementId;
import dev.nthings.tessera.core.FluentBuilder;
import dev.nthings.tessera.core.Fnv1a64;
import dev.nthings.tessera.core.RenderView;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Tokens;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Accessible keyboard-scrollable code block backed by CSS counter line numbers ({@code
 * counter-increment: code-line}), line highlighting, diff annotations, and optional header bars.
 */
public final class CodeBlock implements RenderView, FluentBuilder<CodeBlock> {

  public enum DiffType {
    NONE,
    ADD,
    DELETE
  }

  private final List<String> lines = new ArrayList<>();
  private final Set<Integer> highlightedLines = new LinkedHashSet<>();
  private final Set<Integer> addedLines = new LinkedHashSet<>();
  private final Set<Integer> deletedLines = new LinkedHashSet<>();
  private @Nullable String filename;
  private @Nullable String language;
  private boolean diffAutoDetect = false;
  private final String instanceId;

  public CodeBlock(List<String> lines) {
    this.lines.addAll(lines);
    this.instanceId = "code-" + Fnv1a64.toBase36Hash(System.nanoTime());
  }

  public static CodeBlock of(String code) {
    if (code == null) {
      return new CodeBlock(List.of(""));
    }
    String normalized = code.replace("\r\n", "\n").replace('\r', '\n');
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
    for (int line : lineNumbers) {
      this.highlightedLines.add(line);
    }
    return this;
  }

  public CodeBlock addedLines(int... lineNumbers) {
    for (int line : lineNumbers) {
      this.addedLines.add(line);
    }
    return this;
  }

  public CodeBlock deletedLines(int... lineNumbers) {
    for (int line : lineNumbers) {
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
  public ElementId entityId() {
    return ElementId.of(instanceId);
  }

  @Override
  public void configureScope(ScopeStyle scope) {
    scope
        .root(
            s ->
                s.my(CssLength.rem(1.25))
                    .rounded(CssLength.rem(0.5))
                    .borderSubtle(Tokens.BORDER_SUBTLE)
                    .bg(Tokens.SURFACE_CARD)
                    .overflowClip())
        .select(
            ":scope > .code-header",
            s ->
                s.flex()
                    .justifyBetween()
                    .itemsCenter()
                    .px(CssLength.rem(1))
                    .py(CssLength.rem(0.45))
                    .bg(Tokens.SURFACE_SUBTLE)
                    .borderBlockEndSubtle(Tokens.BORDER_SUBTLE)
                    .fontSize(CssLength.rem(0.8))
                    .textColor(Tokens.TEXT_MUTED))
        .select(
            ":scope > .code-header > .code-filename",
            s -> s.fontWeight(600).textColor(Tokens.TEXT_PRIMARY))
        .select(
            ":scope > .code-pre",
            s ->
                s.m(CssLength.px(0))
                    .py(CssLength.rem(0.75))
                    .bg(Tokens.SURFACE_CARD)
                    .fontSize(CssLength.rem(0.85))
                    .lineHeight("1.6"))
        .select(":scope > .code-pre > .code-lines", s -> s.block())
        .select(
            ":scope > .code-pre > .code-lines > .code-line",
            s -> s.block().counterIncrement("code-line").whiteSpace("pre").px(CssLength.rem(1)))
        .select(
            ":scope > .code-pre > .code-lines > .code-line::before",
            s ->
                s.content("counter(code-line)")
                    .inlineFlex()
                    .justifyEnd()
                    .inlineSize(CssLength.rem(2))
                    .me(CssLength.rem(1.25))
                    .textColor(Tokens.TEXT_MUTED)
                    .opacity(0.6))
        .select(
            ":scope > .code-pre > .code-lines > .code-line.is-highlighted",
            s -> s.bgRaw("color-mix(in oklch, var(--accent-primary) 15%, transparent)"))
        .select(
            ":scope > .code-pre > .code-lines > .code-line.is-add",
            s ->
                s.bgRaw("color-mix(in oklch, var(--status-success) 16%, transparent)")
                    .textColor(Tokens.STATUS_SUCCESS))
        .select(
            ":scope > .code-pre > .code-lines > .code-line.is-del",
            s ->
                s.bgRaw("color-mix(in oklch, var(--status-danger) 16%, transparent)")
                    .textColor(Tokens.STATUS_DANGER));
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
        header.child(span().attr("class", "code-lang").text(language));
      }
      wrapper.child(header);
    }

    var codeEl = code().attr("class", "code-lines");

    for (int i = 0; i < lines.size(); i++) {
      int lineNum = i + 1;
      String rawLine = lines.get(i);
      String content = rawLine.isEmpty() ? " " : rawLine;

      boolean isAdd = addedLines.contains(lineNum);
      boolean isDel = deletedLines.contains(lineNum);
      if (diffAutoDetect) {
        if (rawLine.startsWith("+")) {
          isAdd = true;
        } else if (rawLine.startsWith("-")) {
          isDel = true;
        }
      }
      boolean isHighlight = highlightedLines.contains(lineNum);

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
            .attr("tabindex", "0")
            .refine(
                s ->
                    s.fontFamily("ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace")
                        .overflowXAuto()
                        .counterReset("code-line"))
            .child(codeEl);

    // CSS counter increment on lines
    return wrapper.child(preEl);
  }
}
