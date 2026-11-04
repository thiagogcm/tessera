package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.aside;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.h2;
import static dev.nthings.tessera.core.Elements.h3;
import static dev.nthings.tessera.core.Elements.h4;
import static dev.nthings.tessera.core.Elements.li;
import static dev.nthings.tessera.core.Elements.nav;
import static dev.nthings.tessera.core.Elements.ol;
import static dev.nthings.tessera.core.Elements.span;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementId;
import dev.nthings.tessera.core.Fnv1a64;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.RenderOnce;
import dev.nthings.tessera.core.RenderView;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.HighlightTone;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Tokens;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Zero-JavaScript Scroll-Spy Table of Contents and documentation section primitives backed by
 * Chrome 156+ {@code scroll-target-group: auto} and {@code :target-current}.
 */
public final class Toc {

  private Toc() {}

  /** Explicit Table of Contents target entry. */
  public record Item(String targetDomId, String title, int level) {
    public Item {
      Fnv1a64.sanitizeIdentifier(targetDomId);
      if (level < 2 || level > 4) {
        throw new IllegalArgumentException("Heading level must be between 2 and 4");
      }
    }

    public static Item h2(String targetDomId, String title) {
      return new Item(targetDomId, title, 2);
    }

    public static Item h3(String targetDomId, String title) {
      return new Item(targetDomId, title, 3);
    }
  }

  /**
   * Documentation section that automatically registers its heading with {@link RenderContext}
   * during Phase 1 ({@code requestLayout}) so {@link TableOfContents#fromContext} can populate
   * links without manual duplication.
   */
  public record DocSection(String id, String title, int level, List<Element> body)
      implements RenderOnce {

    public DocSection {
      Fnv1a64.sanitizeIdentifier(id);
      if (level < 2 || level > 4) {
        throw new IllegalArgumentException("DocSection level must be between 2 and 4");
      }
      body = List.copyOf(body);
    }

    @Override
    public Element render() {
      var entry = RenderContext.current().registerTocEntry(id, title, level);
      TagElement heading =
          switch (level) {
            case 2 -> h2().textSize(CssLength.rem(1.375)).fontWeight(700).text(title);
            case 3 -> h3().textSize(CssLength.rem(1.125)).fontWeight(600).text(title);
            default -> h4().textSize(CssLength.rem(1)).fontWeight(600).text(title);
          };

      var sectionEl =
          dev.nthings.tessera.core.Elements.section()
              .attr("id", entry.targetDomId())
              .scrollMarginBlockStart(CssLength.rem(7))
              .flex()
              .flexCol()
              .gap(CssLength.rem(0.75))
              .child(heading);
      for (var child : body) {
        sectionEl.child(child);
      }
      return sectionEl;
    }
  }

  public static DocSection section(String id, String title, IntoElement... children) {
    return createSection(id, title, 2, children);
  }

  public static DocSection subsection(String id, String title, IntoElement... children) {
    return createSection(id, title, 3, children);
  }

  private static DocSection createSection(
      String id, String title, int level, IntoElement... children) {
    var list = new ArrayList<Element>(children.length);
    for (var child : children) {
      list.add(child.intoElement());
    }
    return new DocSection(id, title, level, list);
  }

  public static TableOfContents fromContext(String id, String heading) {
    return new TableOfContents(ElementId.of(id), heading, null);
  }

  public static TableOfContents of(String id, String heading, List<Item> explicitItems) {
    return new TableOfContents(ElementId.of(id), heading, List.copyOf(explicitItems));
  }

  public static DocLayout docLayout(String tocId, String tocHeading, IntoElement... sections) {
    var list = new ArrayList<Element>(sections.length);
    for (var s : sections) {
      list.add(s.intoElement());
    }
    return new DocLayout(tocId, tocHeading, list);
  }

  /**
   * Scroll-Spy Table of Contents {@link RenderView} emitting {@code scroll-target-group: auto} on
   * the {@code <nav>} container and styling active links via {@code a:target-current}.
   */
  public static final class TableOfContents implements RenderView {

    private final ElementId id;
    private final String heading;
    private final @Nullable List<Item> explicitItems;

    public TableOfContents(ElementId id, String heading, @Nullable List<Item> explicitItems) {
      this.id = id;
      this.heading = heading;
      this.explicitItems = explicitItems;
    }

    @Override
    public ElementId entityId() {
      return id;
    }

    @Override
    public String viewType() {
      return "table-of-contents";
    }

    @Override
    public void configureScope(ScopeStyle scope) {
      scope
          .root(
              s ->
                  s.scrollTargetGroupAuto()
                      .sticky()
                      .insetBlockStart(CssLength.rem(6.75))
                      .flex()
                      .flexCol()
                      .gap(CssLength.rem(0.625))
                      .p(CssLength.rem(1))
                      .bg(Tokens.SURFACE_CARD)
                      .borderSubtle(Tokens.BORDER_SUBTLE)
                      .rounded(CssLength.rem(0.75)))
          .select(
              ":scope ol",
              s -> s.listStyleNone().flex().flexCol().gap(CssLength.rem(0.125)).p(CssLength.px(0)))
          .select(
              ":scope a",
              s ->
                  s.block()
                      .py(CssLength.rem(0.375))
                      .px(CssLength.rem(0.75))
                      .textColor(Tokens.TEXT_MUTED)
                      .textSize(CssLength.rem(0.875))
                      .borderInlineStartSubtle(Tokens.BORDER_SUBTLE)
                      .rounded(CssLength.rem(0.25))
                      .refine(
                          r ->
                              r.put(
                                  new dev.nthings.tessera.css.CssDeclaration.TextDecorationLine(
                                      "none"))))
          .select(
              ":scope a[data-level=\"3\"]",
              s -> s.ps(CssLength.rem(1.5)).textSize(CssLength.rem(0.8125)))
          .select(":scope a:hover", s -> s.textColor(Tokens.TEXT_PRIMARY).bg(Tokens.SURFACE_SUBTLE))
          .select(
              ":scope a:target-current",
              s ->
                  s.textColor(Tokens.ACCENT_PRIMARY)
                      .fontWeight(600)
                      .borderInlineStart("3px solid " + Tokens.ACCENT_PRIMARY.varReference())
                      .bgRaw(HighlightTone.ACCENT.toCssBackground()));
    }

    @Override
    public Element render() {
      List<Item> resolvedItems;
      if (explicitItems != null) {
        resolvedItems = explicitItems;
      } else {
        var contextEntries = RenderContext.current().tocEntries();
        resolvedItems = new ArrayList<>(contextEntries.size());
        for (var e : contextEntries) {
          resolvedItems.add(new Item(e.targetDomId(), e.title(), e.level()));
        }
      }

      var list = ol();
      for (var item : resolvedItems) {
        list.child(
            li().child(
                    a("#" + item.targetDomId())
                        .attr("data-level", Integer.toString(item.level()))
                        .text(item.title())));
      }

      return nav()
          .ariaLabel(heading)
          .children(
              span()
                  .textSize(CssLength.rem(0.75))
                  .fontWeight(700)
                  .textColor(Tokens.TEXT_MUTED)
                  .text(heading),
              list);
    }
  }

  /**
   * Two-column documentation layout that expands the prose sections before rendering the sticky
   * {@link TableOfContents} sidebar so all section headings are registered in {@link RenderContext}
   * during Phase 1.
   */
  public record DocLayout(String tocId, String tocHeading, List<Element> sections)
      implements RenderView {

    public DocLayout {
      sections = List.copyOf(sections);
    }

    @Override
    public ElementId entityId() {
      return ElementId.of(tocId + "-layout");
    }

    @Override
    public String viewType() {
      return "doc-layout";
    }

    @Override
    public void configureScope(ScopeStyle scope) {
      scope
          .root(s -> s.flex().flexCol().gap(CssLength.rem(2)).containerInlineSize())
          .select(":scope > .doc-grid", s -> s.flex().flexCol().gap(CssLength.rem(2)))
          .containerMinInlineSize(
              CssLength.rem(42),
              c ->
                  c.select(
                          ":scope > .doc-grid",
                          s ->
                              s.grid()
                                  .gridColumns("minmax(0, 1fr) 15.5rem")
                                  .itemsStart()
                                  .gap(CssLength.rem(2)))
                      .select(
                          ":scope > .doc-grid > aside",
                          s -> s.sticky().insetBlockStart(CssLength.rem(6.75))));
    }

    @Override
    public Element render() {
      var proseCol = div().flex().flexCol().gap(CssLength.rem(2.5));
      for (var sec : sections) {
        proseCol.child(sec);
      }
      var sidebar = aside().child(fromContext(tocId, tocHeading));
      return div().child(div().attr("class", "doc-grid").children(proseCol, sidebar));
    }
  }
}
