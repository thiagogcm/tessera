package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.input;
import static dev.nthings.tessera.core.Elements.label;
import static dev.nthings.tessera.core.Elements.li;
import static dev.nthings.tessera.core.Elements.nav;
import static dev.nthings.tessera.core.Elements.span;
import static dev.nthings.tessera.core.Elements.ul;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementId;
import dev.nthings.tessera.core.Fnv1a64;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderView;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Tokens;
import java.util.List;

/**
 * Navigation and in-page content tab components backed by CSS Anchor Positioning ({@code
 * anchor-name}, {@code position-anchor}, {@code anchor(left)}, {@code anchor(right)}, {@code
 * anchor(bottom)}) for animated active tab underlines and {@code :has(> input:checked)} for
 * zero-JavaScript panel switching.
 */
public final class Tabs {

  private Tabs() {}

  public record NavItem(String href, String label) {}

  public record TabPanel(String label, Element content) {
    public static TabPanel of(String label, IntoElement content) {
      return new TabPanel(label, content.intoElement());
    }
  }

  public static NavTabs navTabs(
      String id, String ariaLabel, String activeHref, List<NavItem> items) {
    return new NavTabs(ElementId.of(id), ariaLabel, activeHref, items);
  }

  public static ContentTabs contentTabs(String id, List<TabPanel> panels) {
    return new ContentTabs(ElementId.of(id), panels);
  }

  /**
   * Route navigation tab bar using CSS Anchor Positioning ({@code ul::before} tethered to {@code
   * li.active}) and {@code aria-current="page"}.
   */
  public static final class NavTabs implements RenderView {

    private final ElementId id;
    private final String ariaLabel;
    private final String anchorIdent;
    private final String activeHref;
    private final List<NavItem> items;

    public NavTabs(ElementId id, String ariaLabel, String activeHref, List<NavItem> items) {
      this.id = id;
      this.ariaLabel = ariaLabel;
      this.anchorIdent = "--active-nav-" + id.slug();
      this.activeHref = activeHref;
      this.items = List.copyOf(items);
    }

    @Override
    public ElementId entityId() {
      return id;
    }

    @Override
    public String viewType() {
      return "nav-tabs";
    }

    @Override
    public void configureScope(ScopeStyle scope) {
      scope
          .root(s -> s.inlineFlex())
          .select(
              ":scope ul",
              s ->
                  s.listStyleNone()
                      .flex()
                      .itemsCenter()
                      .gap(CssLength.rem(0.25))
                      .relative()
                      .p(CssLength.px(0))
                      .m(CssLength.px(0)))
          .select(":scope li.active", s -> s.anchorName(anchorIdent))
          .select(
              ":scope ul::before",
              s ->
                  s.cssContent("\"\"")
                      .absolute()
                      .positionAnchor(anchorIdent)
                      .insetBlockStart(CssLength.expr("calc(anchor(bottom) - 2px)"))
                      .insetInlineStart(CssLength.expr("anchor(left)"))
                      .insetInlineEnd(CssLength.expr("anchor(right)"))
                      .blockSize(CssLength.px(2))
                      .bg(Tokens.ACCENT_PRIMARY))
          .motionSafe(m -> m.select(":scope ul::before", s -> s.transition("inset 0.2s ease")))
          .supportsNot(
              "position-anchor: auto",
              f ->
                  f.select(
                      ":scope ul li.active",
                      s ->
                          s.refine(
                              r ->
                                  r.put(
                                      new dev.nthings.tessera.css.CssDeclaration.BorderBlockEnd(
                                          "2px solid " + Tokens.ACCENT_PRIMARY.varReference())))))
          .select(
              ":scope a",
              s ->
                  s.block()
                      .px(CssLength.rem(0.875))
                      .py(CssLength.rem(0.5))
                      .rounded(CssLength.rem(0.375))
                      .textColor(Tokens.TEXT_MUTED)
                      .fontWeight(500)
                      .refine(
                          r ->
                              r.put(
                                  new dev.nthings.tessera.css.CssDeclaration.TextDecorationLine(
                                      "none"))))
          .select(":scope a:hover", s -> s.textColor(Tokens.TEXT_PRIMARY).bg(Tokens.SURFACE_SUBTLE))
          .select(":scope li.active > a", s -> s.textColor(Tokens.ACCENT_PRIMARY).fontWeight(600));
    }

    @Override
    public Element render() {
      var list = ul();
      for (var item : items) {
        boolean isActive = item.href().equals(activeHref);
        var link =
            a(item.href()).when(isActive, el -> el.attr("aria-current", "page")).text(item.label());
        var liEl = li().when(isActive, el -> el.attr("class", "active")).child(link);
        list.child(liEl);
      }
      return nav().ariaLabel(ariaLabel).child(list);
    }
  }

  /**
   * Zero-JavaScript in-page tabbed content widget that switches panels via {@code :has(>
   * input:checked)} and smoothly slides its active underline across tabs using CSS Anchor
   * Positioning ({@code transition: inset 0.2s ease}).
   */
  public static final class ContentTabs implements RenderView {

    private final ElementId id;
    private final String radioGroupName;
    private final String anchorIdent;
    private final List<TabPanel> panels;

    public ContentTabs(ElementId id, List<TabPanel> panels) {
      if (panels.isEmpty()) {
        throw new IllegalArgumentException("ContentTabs requires at least 1 TabPanel");
      }
      this.id = id;
      this.radioGroupName = "tabs-" + Fnv1a64.sanitizeIdentifier(id.slug());
      this.anchorIdent = "--active-tab-" + Fnv1a64.sanitizeIdentifier(id.slug());
      this.panels = List.copyOf(panels);
    }

    @Override
    public ElementId entityId() {
      return id;
    }

    @Override
    public String viewType() {
      return "content-tabs";
    }

    @Override
    public void configureScope(ScopeStyle scope) {
      scope
          .root(
              s ->
                  s.flex()
                      .flexCol()
                      .rounded(CssLength.rem(0.75))
                      .bg(Tokens.SURFACE_CARD)
                      .borderSubtle(Tokens.BORDER_SUBTLE)
                      .overflowClip())
          .select(
              ":scope > .tab-bar",
              s ->
                  s.flex()
                      .itemsCenter()
                      .gap(CssLength.rem(0.25))
                      .px(CssLength.rem(1))
                      .pt(CssLength.rem(0.5))
                      .relative()
                      .borderBlockEndSubtle(Tokens.BORDER_SUBTLE)
                      .bg(Tokens.SURFACE_SUBTLE))
          .select(
              ":scope > .tab-bar > .tab-label",
              s ->
                  s.inlineFlex()
                      .itemsCenter()
                      .px(CssLength.rem(1))
                      .py(CssLength.rem(0.625))
                      .textColor(Tokens.TEXT_MUTED)
                      .fontWeight(500)
                      .cursorPointer())
          .select(":scope > .tab-bar > .tab-label > input[type=\"radio\"]", s -> s.hidden())
          .select(
              ":scope > .tab-bar > .tab-label:has(> input:checked)",
              s -> s.anchorName(anchorIdent).textColor(Tokens.ACCENT_PRIMARY).fontWeight(600))
          .select(
              ":scope > .tab-bar::before",
              s ->
                  s.cssContent("\"\"")
                      .absolute()
                      .positionAnchor(anchorIdent)
                      .insetBlockStart(CssLength.expr("calc(anchor(bottom) - 2px)"))
                      .insetInlineStart(CssLength.expr("anchor(left)"))
                      .insetInlineEnd(CssLength.expr("anchor(right)"))
                      .blockSize(CssLength.px(2))
                      .bg(Tokens.ACCENT_PRIMARY))
          .motionSafe(
              m -> m.select(":scope > .tab-bar::before", s -> s.transition("inset 0.2s ease")))
          .supportsNot(
              "position-anchor: auto",
              f ->
                  f.select(
                      ":scope > .tab-bar > .tab-label:has(> input:checked)",
                      s ->
                          s.refine(
                              r ->
                                  r.put(
                                      new dev.nthings.tessera.css.CssDeclaration.BorderBlockEnd(
                                          "2px solid " + Tokens.ACCENT_PRIMARY.varReference())))))
          .select(":scope > .tab-panel", s -> s.hidden().p(CssLength.rem(1.25)));

      for (int i = 0; i < panels.size(); i++) {
        int tabIndex = i + 1;
        scope.select(
            ":scope:has(> .tab-bar > .tab-label[data-tab=\""
                + tabIndex
                + "\"] > input:checked) > .tab-panel[data-panel=\""
                + tabIndex
                + "\"]",
            s -> s.block());
      }
    }

    @Override
    public Element render() {
      var tabBar = div().attr("class", "tab-bar");
      var root = div().child(tabBar);

      for (int i = 0; i < panels.size(); i++) {
        int tabIndex = i + 1;
        var panel = panels.get(i);
        var radio =
            input("radio")
                .attr("name", radioGroupName)
                .attr("value", Integer.toString(tabIndex))
                .boolAttr("checked", i == 0);
        tabBar.child(
            label()
                .attr("class", "tab-label")
                .attr("data-tab", Integer.toString(tabIndex))
                .children(radio, span().text(panel.label())));
        root.child(
            div()
                .attr("class", "tab-panel")
                .attr("data-panel", Integer.toString(tabIndex))
                .child(panel.content()));
      }
      return root;
    }
  }
}
