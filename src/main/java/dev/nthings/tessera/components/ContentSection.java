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
import dev.nthings.tessera.core.Fnv1a64;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.RenderOnce;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.Tokens;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/// Semantic `<section>` container with configurable surface elevations ([SurfaceKind]), automatic
/// Table of Contents registration, and optional lead text and badges.
public final class ContentSection implements RenderOnce {

  /// Surface presentation styling.
  public enum SurfaceKind {
    /// Clean borderless continuous narrative flow (default for docs).
    GHOST("ghost"),
    /// Subtle flat surface with bottom border divider.
    FLAT("flat"),
    /// Framed card surface with subtle border and shadow (for interactive widgets and forms).
    CARD("card"),
    /// Accent-tinted OKLCH highlight surface for key announcements and callouts.
    HIGHLIGHT("highlight");

    private final String value;

    SurfaceKind(String value) {
      this.value = value;
    }

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

  public ContentSection(String id, String title) {
    this.rawId = Fnv1a64.sanitizeIdentifier(id);
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
    if (headingLevel < 2 || headingLevel > 4) {
      throw new IllegalArgumentException("Heading level must be between 2 and 4");
    }
    this.level = headingLevel;
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

  public ContentSection child(IntoElement child) {
    this.children.add(child.intoElement());
    return this;
  }

  public ContentSection children(IntoElement... children) {
    for (var child : children) {
      this.children.add(child.intoElement());
    }
    return this;
  }

  @Override
  public Element render() {
    var entry = RenderContext.current().registerTocEntry(rawId, title, level);

    TagElement heading =
        switch (level) {
          case 2 ->
              h2().attr("id", entry.targetDomId() + "--heading")
                  .textSize(CssLength.rem(1.625))
                  .fontWeight(700)
                  .text(title);
          case 3 ->
              h3().attr("id", entry.targetDomId() + "--heading")
                  .textSize(CssLength.rem(1.25))
                  .fontWeight(600)
                  .text(title);
          default ->
              h4().attr("id", entry.targetDomId() + "--heading")
                  .textSize(CssLength.rem(1.05))
                  .fontWeight(600)
                  .text(title);
        };

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
    for (var child : children) {
      bodyContainer.child(child);
    }

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
}
