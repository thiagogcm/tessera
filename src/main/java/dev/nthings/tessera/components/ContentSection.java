package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.h2;
import static dev.nthings.tessera.core.Elements.h3;
import static dev.nthings.tessera.core.Elements.h4;
import static dev.nthings.tessera.core.Elements.header;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.core.Elements.section;
import static dev.nthings.tessera.core.Elements.span;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ParentElement;
import dev.nthings.tessera.core.RenderOnce;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.Tokens;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.jspecify.annotations.Nullable;

/// Semantic `<section>` container with configurable surface elevations ([SurfaceKind]), automatic
/// Table of Contents registration, and optional lead text and badges.
public final class ContentSection implements RenderOnce, ParentElement<ContentSection> {

  /// Surface presentation styling.
  public enum SurfaceKind {
    /// Clean borderless continuous narrative flow (default for docs).
    GHOST,
    /// Subtle flat surface with bottom border divider.
    FLAT,
    /// Framed card surface with subtle border and shadow (for interactive widgets and forms).
    CARD,
    /// Accent-tinted OKLCH highlight surface for key announcements and callouts.
    HIGHLIGHT;

    private final String value = name().toLowerCase(Locale.ROOT).replace('_', '-');

    public String wireValue() {
      return value;
    }
  }

  private final String rawId;
  private final String title;
  private int level = 2;
  private SurfaceKind surface = SurfaceKind.GHOST;
  private @Nullable String leadText;
  private @Nullable String badge;
  private final List<Element> children = new ArrayList<>();

  private ContentSection(String id, String title) {
    this.rawId = id;
    this.title = title;
  }

  public static ContentSection create(String id, String title) {
    return new ContentSection(id, title);
  }

  public static ContentSection ghost(String id, String title) {
    return new ContentSection(id, title).surface(SurfaceKind.GHOST);
  }

  public static ContentSection card(String id, String title) {
    return new ContentSection(id, title).surface(SurfaceKind.CARD);
  }

  public static ContentSection highlight(String id, String title) {
    return new ContentSection(id, title).surface(SurfaceKind.HIGHLIGHT);
  }

  public ContentSection level(int headingLevel) {
    this.level = checkHeadingLevel(headingLevel);
    return this;
  }

  public ContentSection surface(SurfaceKind kind) {
    this.surface = kind;
    return this;
  }

  public ContentSection leadText(String lead) {
    this.leadText = lead;
    return this;
  }

  public ContentSection badge(String badgeText) {
    this.badge = badgeText;
    return this;
  }

  @Override
  public ContentSection self() {
    return this;
  }

  @Override
  public void appendChild(Element child) {
    children.add(child);
  }

  @Override
  public Element render() {
    var entry = Toc.register(rawId, title, level);

    TagElement heading =
        sectionHeading(
            level, entry.targetDomId(), title, level == 2 ? 1.625 : level == 3 ? 1.25 : 1.05);

    var headerEl =
        header().attr("class", "section-header").flex().flexCol().gap(CssLength.rem(0.35));
    if (badge != null) {
      headerEl.child(
          span()
              .attr("class", "section-badge")
              .inlineFlex()
              .fontSize(CssLength.rem(0.75))
              .fontWeight(700)
              .textColor(Tokens.ACCENT_PRIMARY)
              .text(badge));
    }
    headerEl.child(heading);
    if (leadText != null) {
      headerEl.child(
          p().attr("class", "section-lead")
              .fontSize(CssLength.rem(1.1))
              .textColor(Tokens.TEXT_MUTED)
              .lineHeight("1.6")
              .m(CssLength.px(0))
              .text(leadText));
    }

    var bodyContainer = div().attr("class", "section-body").flex().flexCol().gap(CssLength.rem(1));
    bodyContainer.children(children);

    var sectionEl =
        section()
            .attr("id", entry.targetDomId())
            .attr("data-surface", surface.wireValue())
            .attr("class", "content-section-node")
            .flex()
            .flexCol()
            .gap(CssLength.rem(1.25))
            .scrollMarginBlockStart(CssLength.rem(7));

    switch (surface) {
      case GHOST -> sectionEl.p(CssLength.px(0)).bgRaw("transparent").borderNone();
      case FLAT -> sectionEl.py(CssLength.rem(1.5)).borderBlockEndSubtle(Tokens.BORDER_SUBTLE);
      case CARD ->
          sectionEl
              .p(CssLength.rem(1.5))
              .rounded(CssLength.rem(0.75))
              .bg(Tokens.SURFACE_CARD)
              .borderSubtle(Tokens.BORDER_SUBTLE)
              .shadow("0 4px 16px -4px oklch(0% 0 0 / 0.12)");
      case HIGHLIGHT ->
          sectionEl
              .p(CssLength.rem(1.5))
              .rounded(CssLength.rem(0.75))
              .bgRaw("oklch(from " + Tokens.ACCENT_PRIMARY.varReference() + " 97% 0.03 h)")
              .borderSubtle(Tokens.ACCENT_PRIMARY);
    }

    return sectionEl.child(headerEl).child(bodyContainer);
  }

  static int checkHeadingLevel(int level) {
    if (level < 2 || level > 4) {
      throw new IllegalArgumentException("Heading level must be between 2 and 4");
    }
    return level;
  }

  /// Builds the `<h2>`–`<h4>` heading for a TOC-registered section, id'd `<sectionDomId>--heading`.
  static TagElement sectionHeading(int level, String sectionDomId, String title, double sizeRem) {
    TagElement heading =
        switch (level) {
          case 2 -> h2();
          case 3 -> h3();
          default -> h4();
        };
    return heading
        .attr("id", sectionDomId + "--heading")
        .fontSize(CssLength.rem(sizeRem))
        .fontWeight(level == 2 ? 700 : 600)
        .text(title);
  }
}
