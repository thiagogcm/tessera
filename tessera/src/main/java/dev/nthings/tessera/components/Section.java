package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.header;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.core.Elements.section;
import static dev.nthings.tessera.core.Elements.span;

import dev.nthings.tessera.core.DocumentContext;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.ParentElement;
import dev.nthings.tessera.css.Border;
import dev.nthings.tessera.css.CssColor;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.Shadow;
import dev.nthings.tessera.css.Tokens;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/// Titled `<section>` whose heading joins the document outline, so [TableOfContents] and the site
/// search index list it. Create one with [#of(String, String)] and add its content as children.
/// Its `id` is a page-level DOM id ([ElementRef#page(String)]) on the `<section>`, so
/// `/guide/#installation` links to the section from any page; the heading points the outline at
/// that same id. Two sections with one id on a page report `duplicate-id`, and placing one
/// instance twice on a page reports `duplicate-reference-binding`. The heading is a [Heading].
/// Levels are 2 through 4. Use [Heading] directly for levels 1 through 6 or a rich title.
public final class Section extends BuiltinComponent implements ParentElement<Section> {

  /// Visual surface of a [Section].
  public enum Surface {
    /// Borderless, continuous flow. Default.
    GHOST,
    /// Flat surface with a bottom divider.
    FLAT,
    /// Bordered, shadowed card for widgets and forms.
    CARD,
    /// Accent-tinted surface for key announcements.
    HIGHLIGHT
  }

  private final ElementRef target;
  private final String title;
  private int level = 2;
  private Surface surface = Surface.GHOST;
  private @Nullable String description;
  private @Nullable String eyebrow;
  private final List<Element> children = new ArrayList<>();

  private Section(ElementRef target, String title) {
    this.target = target;
    this.title = title;
  }

  /// Creates a level-2 section with the `GHOST` surface, page-level id `id` and visible heading
  /// `title`. Throws IllegalArgumentException if `id` is empty or contains whitespace.
  public static Section of(String id, String title) {
    return new Section(ElementRef.page(id), title);
  }

  /// Sets the heading level, 2 to 4 (default 2). A table of contents indents level-3 entries under
  /// the section before them. Throws IllegalArgumentException outside that range.
  public Section level(int level) {
    if (level < 2 || level > 4) {
      throw new IllegalArgumentException("Section heading level must be 2 to 4: " + level);
    }
    this.level = level;
    return this;
  }

  /// Sets the surface. The default is `GHOST`.
  public Section surface(Surface surface) {
    this.surface = surface;
    return this;
  }

  /// Sets a larger muted paragraph shown under the heading.
  public Section description(String description) {
    this.description = description;
    return this;
  }

  /// Sets a small accent label shown above the heading.
  public Section eyebrow(String eyebrow) {
    this.eyebrow = eyebrow;
    return this;
  }

  @Override
  public Section self() {
    return this;
  }

  @Override
  public void appendChild(Element child) {
    children.add(child);
  }

  @Override
  IntoElement expand(DocumentContext context) {
    var heading =
        Heading.of(level, title)
            .outline(target)
            .present(
                tag ->
                    tag.fontSize(
                            level == 2
                                ? Tokens.FONT_SIZE_2XL
                                : level == 3 ? Tokens.FONT_SIZE_XL : Tokens.FONT_SIZE_LG)
                        .fontWeight(level == 2 ? 700 : 600)
                        .m(CssLength.px(0)));

    var headerEl =
        header().attr("class", "section-header").flex().flexCol().gap(CssLength.rem(0.35));
    if (eyebrow != null) {
      headerEl.child(
          span()
              .attr("class", "section-badge")
              .inlineFlex()
              .fontSize(Tokens.FONT_SIZE_XS)
              .fontWeight(700)
              .textColor(Tokens.ACCENT_PRIMARY)
              .text(eyebrow));
    }
    headerEl.child(heading);
    if (description != null) {
      headerEl.child(
          p().attr("class", "section-lead")
              .fontSize(Tokens.FONT_SIZE_LG)
              .textColor(Tokens.TEXT_MUTED)
              .lineHeight(1.6)
              .m(CssLength.px(0))
              .text(description));
    }

    var bodyContainer = div().attr("class", "section-body").flex().flexCol().gap(Tokens.SPACE_4);
    bodyContainer.children(children);

    var sectionEl =
        section()
            .id(target)
            .attr("data-surface", CssNames.of(surface))
            .attr("class", "content-section-node")
            .flex()
            .flexCol()
            .gap(Tokens.SPACE_5)
            .scrollMarginBlockStart(CssLength.rem(7));

    switch (surface) {
      case GHOST -> sectionEl.p(CssLength.px(0)).bg(CssColor.transparent()).border(Border.none());
      case FLAT ->
          sectionEl.py(Tokens.SPACE_6).borderBlockEnd(Border.hairline(Tokens.BORDER_SUBTLE));
      case CARD ->
          sectionEl
              .p(Tokens.SPACE_6)
              .rounded(Tokens.RADIUS_XL)
              .bg(Tokens.SURFACE_CARD)
              .border(Border.hairline(Tokens.BORDER_SUBTLE))
              .shadow(
                  Shadow.drop(
                      CssLength.px(4),
                      CssLength.px(16),
                      CssLength.px(-4),
                      CssColor.oklch(0, 0, 0, 0.12)));
      case HIGHLIGHT ->
          sectionEl
              .p(Tokens.SPACE_6)
              .rounded(Tokens.RADIUS_XL)
              .bg(CssColor.sameHue(Tokens.ACCENT_PRIMARY, 97, 0.03))
              .border(Border.hairline(Tokens.ACCENT_PRIMARY));
    }

    return sectionEl.child(headerEl).child(bodyContainer);
  }
}
