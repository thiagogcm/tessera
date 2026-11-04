package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.button;
import static dev.nthings.tessera.core.Elements.code;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.pre;
import static dev.nthings.tessera.core.Elements.rawHtml;
import static dev.nthings.tessera.core.Elements.span;

import dev.nthings.tessera.core.Action;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.FluentBuilder;
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

/// Accessible keyboard-scrollable code block backed by CSS counter line numbers
/// (`counter-increment: code-line`), line highlighting, diff annotations, declarative copy buttons,
/// and optional header bars.
public final class CodeBlock implements RenderView, FluentBuilder<CodeBlock> {

  private static final Action COPY_ACTION = Action.of("tessera", "copy");

  private final List<String> lines = new ArrayList<>();
  private final Set<Integer> highlightedLines = new LinkedHashSet<>();
  private final Set<Integer> addedLines = new LinkedHashSet<>();
  private final Set<Integer> deletedLines = new LinkedHashSet<>();
  private @Nullable String filename;
  private @Nullable String language;
  private boolean diffAutoDetect = false;
  private boolean copyButton = true;

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

  public CodeBlock copyButton(boolean enabled) {
    this.copyButton = enabled;
    return this;
  }

  public boolean hasCopyButton() {
    return copyButton;
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
                s.relative()
                    .my(CssLength.rem(1.25))
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
            ":scope > .code-header > .code-header-actions",
            s -> s.flex().itemsCenter().gap(CssLength.rem(0.5)))
        .select(
            ":scope .code-copy-btn",
            s ->
                s.inlineFlex()
                    .itemsCenter()
                    .gap(CssLength.rem(0.35))
                    .px(CssLength.rem(0.5))
                    .py(CssLength.rem(0.25))
                    .rounded(CssLength.rem(0.375))
                    .borderSubtle(Tokens.BORDER_SUBTLE)
                    .bg(Tokens.SURFACE_CARD)
                    .textColor(Tokens.TEXT_MUTED)
                    .fontSize(CssLength.rem(0.75))
                    .lineHeight("1")
                    .cursorPointer()
                    .transition(
                        "background-color 0.15s ease, color 0.15s ease, border-color 0.15s ease"))
        .select(
            ":scope .code-copy-btn:hover",
            s -> s.bg(Tokens.SURFACE_SUBTLE).textColor(Tokens.TEXT_PRIMARY))
        .select(
            ":scope .code-copy-btn.is-floating",
            s ->
                s.absolute()
                    .insetBlockStart(CssLength.rem(0.5))
                    .insetInlineEnd(CssLength.rem(0.5))
                    .zIndex(2)
                    .opacity(0.6)
                    .transition("opacity 0.15s ease"))
        .select(
            ":scope:hover .code-copy-btn.is-floating, :scope:focus-within .code-copy-btn.is-floating",
            s -> s.opacity(1.0))
        .select(
            ":scope .code-copy-btn .copied-icon, :scope .code-copy-btn .copied-text",
            s -> s.hidden())
        .select(
            ":scope .code-copy-btn .copy-icon, :scope .code-copy-btn .copy-text",
            s -> s.inlineBlock())
        .select(
            ":scope .code-copy-btn:active .copy-icon, :scope .code-copy-btn:active .copy-text",
            s -> s.hidden())
        .select(
            ":scope .code-copy-btn:active .copied-icon, :scope .code-copy-btn:active .copied-text",
            s -> s.inlineBlock().textColor(Tokens.STATUS_SUCCESS))
        .select(
            ":scope .code-copy-btn:active",
            s ->
                s.bgRaw(Tokens.STATUS_SUCCESS.mixTransparent(14))
                    .textColor(Tokens.STATUS_SUCCESS)
                    .border("1px solid " + Tokens.STATUS_SUCCESS.mixTransparent(32)))
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
    var codeRef = ElementRef.named("code");

    if (filename != null || language != null) {
      var header = div().attr("class", "code-header");
      if (filename != null) {
        header.child(span().attr("class", "code-filename").text(filename));
      } else {
        header.child(span());
      }
      var actions = div().attr("class", "code-header-actions");
      if (language != null) {
        actions.child(Pill.neutral(language).sm());
      }
      if (copyButton) {
        actions.child(copyButton(codeRef, "code-copy-btn"));
      }
      header.child(actions);
      wrapper.child(header);
    } else if (copyButton) {
      wrapper.child(copyButton(codeRef, "code-copy-btn is-floating"));
    }

    var codeEl = code().attr("class", "code-lines").id(codeRef);

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
                    s.fontFamily("ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace")
                        .overflowXAuto()
                        .counterReset("code-line"))
            .child(codeEl);

    // CSS counter increment on lines
    return wrapper.child(preEl);
  }

  private static Element copyButton(ElementRef target, String cssClass) {
    return button()
        .attr("class", cssClass)
        .ariaLabel("Copy code to clipboard")
        .dispatchAction(target, COPY_ACTION)
        .child(
            rawHtml(
                "<svg class=\"copy-icon\" width=\"13\" height=\"13\" viewBox=\"0 0 16 16\" fill=\"none\""
                    + " stroke=\"currentColor\" stroke-width=\"1.75\" stroke-linecap=\"round\""
                    + " stroke-linejoin=\"round\" aria-hidden=\"true\"><rect x=\"5\" y=\"5\""
                    + " width=\"9\" height=\"9\" rx=\"1.5\"/><path d=\"M11 5V3a1.5 1.5 0 0"
                    + " 0-1.5-1.5H3A1.5 1.5 0 0 0 1.5 3v6.5A1.5 1.5 0 0 0 3 11h2\"/></svg>"))
        .child(
            rawHtml(
                "<svg class=\"copied-icon\" width=\"13\" height=\"13\" viewBox=\"0 0 16 16\" fill=\"none\""
                    + " stroke=\"currentColor\" stroke-width=\"2.2\" stroke-linecap=\"round\""
                    + " stroke-linejoin=\"round\" aria-hidden=\"true\"><polyline points=\"3.5"
                    + " 8.5 6.5 11.5 12.5 4.5\"/></svg>"))
        .child(span().attr("class", "copy-text").text("Copy"))
        .child(span().attr("class", "copied-text").text("Copied!"));
  }
}
