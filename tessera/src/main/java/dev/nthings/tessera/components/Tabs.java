package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.button;
import static dev.nthings.tessera.core.Elements.custom;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.li;
import static dev.nthings.tessera.core.Elements.nav;
import static dev.nthings.tessera.core.Elements.ul;

import dev.nthings.tessera.core.Accessible;
import dev.nthings.tessera.core.ClientBehavior;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementId;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.RenderView;
import dev.nthings.tessera.core.Role;
import dev.nthings.tessera.css.Animatable;
import dev.nthings.tessera.css.CssColor;
import dev.nthings.tessera.css.CssContent;
import dev.nthings.tessera.css.CssDuration;
import dev.nthings.tessera.css.CssIdent;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.Easing;
import dev.nthings.tessera.css.Motion;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.WhiteSpace;
import java.util.List;

/// Navigation and in-page tabs. The active underline uses CSS Anchor Positioning
/// (`anchor-name`, `position-anchor`, `anchor()`). Content tabs follow the ARIA tabs
/// pattern with the bundled `tessera-tabs` behavior (roving tabindex, arrow keys).
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
    return contentTabs(id, "Content tabs", panels);
  }

  public static ContentTabs contentTabs(String id, String ariaLabel, List<TabPanel> panels) {
    return new ContentTabs(ElementId.of(id), ariaLabel, panels);
  }

  /// Route nav bar: `ul::before` is tethered to `li.active`; sets `aria-current="page"`.
  public static final class NavTabs implements RenderView {

    private final ElementId id;
    private final String ariaLabel;
    private final CssIdent anchorIdent;
    private final String activeHref;
    private final List<NavItem> items;

    public NavTabs(ElementId id, String ariaLabel, String activeHref, List<NavItem> items) {
      this.id = id;
      this.ariaLabel = ariaLabel;
      this.anchorIdent = CssIdent.custom("active-nav-" + id.slug());
      this.activeHref = activeHref;
      this.items = List.copyOf(items);
    }

    @Override
    public ElementId entityId() {
      return id;
    }

    @Override
    public void configureScope(ScopeStyle scope) {
      scope
          .root(s -> s.inlineFlex().maxInlineSize(CssLength.pct(100)))
          .select(
              ":scope ul",
              s ->
                  s.listStyleNone()
                      .flex()
                      .flexWrap()
                      .itemsCenter()
                      .gap(Tokens.SPACE_1)
                      .relative()
                      .p(CssLength.px(0))
                      .m(CssLength.px(0)))
          .select(":scope li.active", s -> s.anchorName(anchorIdent));
      anchorUnderline(scope, ":scope ul::before", anchorIdent)
          .select(
              ":scope a",
              s ->
                  s.block()
                      .px(Tokens.SPACE_2_5)
                      .py(Tokens.SPACE_1_5)
                      .rounded(Tokens.RADIUS_MD)
                      .textColor(Tokens.TEXT_MUTED)
                      .fontSize(Tokens.FONT_SIZE_SM)
                      .fontWeight(500)
                      .whiteSpace(WhiteSpace.NOWRAP)
                      .textDecorationNone())
          .select(":scope a:hover", s -> s.textColor(Tokens.TEXT_PRIMARY).bg(Tokens.SURFACE_SUBTLE))
          .select(":scope li.active > a", s -> s.textColor(Tokens.ACCENT_PRIMARY).fontWeight(600));
    }

    @Override
    public Element render(RenderContext context) {
      var list = ul();
      for (var item : items) {
        var isActive = item.href().equals(activeHref);
        var link =
            a(item.href())
                .when(isActive, el -> el.ariaCurrent(Accessible.Current.PAGE))
                .text(item.label());
        var liEl = li().when(isActive, el -> el.attr("class", "active")).child(link);
        list.child(liEl);
      }
      return nav().ariaLabel(ariaLabel).child(list);
    }
  }

  /// In-page ARIA tab set with roving tabindex, driven by `tessera-tabs`. Panels stay
  /// server-rendered; the client only switches selection and focus.
  public static final class ContentTabs implements RenderView {

    private static final ClientBehavior TABS_BEHAVIOR =
        ClientBehavior.fromResource("tessera-tabs", ContentTabs.class, "tessera-tabs.js");

    private final ElementId id;
    private final String ariaLabel;
    private final CssIdent anchorIdent;
    private final List<TabPanel> panels;

    public ContentTabs(ElementId id, String ariaLabel, List<TabPanel> panels) {
      if (panels.isEmpty()) {
        throw new IllegalArgumentException("ContentTabs requires at least 1 TabPanel");
      }
      this.id = id;
      this.ariaLabel = ariaLabel;
      this.anchorIdent = CssIdent.custom("active-tab-" + id.slug());
      this.panels = List.copyOf(panels);
    }

    @Override
    public ElementId entityId() {
      return id;
    }

    @Override
    public void configureScope(ScopeStyle scope) {
      scope
          .root(
              s ->
                  s.flex()
                      .flexCol()
                      .rounded(Tokens.RADIUS_XL)
                      .bg(Tokens.SURFACE_CARD)
                      .borderSubtle(Tokens.BORDER_SUBTLE)
                      .overflowClip())
          .select(
              ":scope > .tab-bar",
              s ->
                  s.flex()
                      .flexWrap()
                      .itemsCenter()
                      .gap(Tokens.SPACE_1)
                      .px(Tokens.SPACE_4)
                      .pt(Tokens.SPACE_2)
                      .relative()
                      .borderBlockEndSubtle(Tokens.BORDER_SUBTLE)
                      .bg(Tokens.SURFACE_SUBTLE))
          .select(
              ":scope > .tab-bar > [role=\"tab\"]",
              s ->
                  s.inlineFlex()
                      .itemsCenter()
                      .m(CssLength.zero())
                      .px(Tokens.SPACE_3_5)
                      .py(Tokens.SPACE_2_5)
                      .bg(CssColor.transparent())
                      .borderNone()
                      .textColor(Tokens.TEXT_MUTED)
                      .fontFamilyInherit()
                      .fontSize(Tokens.FONT_SIZE_SM)
                      .fontWeight(500)
                      .whiteSpace(WhiteSpace.NOWRAP)
                      .cursorPointer())
          .select(
              ":scope > .tab-bar > [role=\"tab\"]:hover",
              s -> s.bg(Tokens.SURFACE_CARD).textColor(Tokens.TEXT_PRIMARY))
          .select(
              ":scope > .tab-bar > [role=\"tab\"]:focus-visible",
              s -> s.focusRing(Tokens.ACCENT_PRIMARY))
          .select(
              ":scope > .tab-bar > [role=\"tab\"][aria-selected=\"true\"]",
              s -> s.anchorName(anchorIdent).textColor(Tokens.ACCENT_PRIMARY).fontWeight(600))
          .select(":scope > [role=\"tabpanel\"]", s -> s.p(Tokens.SPACE_5))
          // Before `tessera-tabs` upgrades, show every panel rather than hiding all but the first.
          .select(":scope:not(:defined) > .tab-bar", s -> s.hidden())
          .select(":scope:not(:defined) > [role=\"tabpanel\"][hidden]", s -> s.block());
      anchorUnderline(scope, ":scope > .tab-bar::before", anchorIdent);
    }

    @Override
    public Element render(RenderContext context) {
      context.contributeBehavior(TABS_BEHAVIOR);

      var tabBar = div().attr("class", "tab-bar").role(Role.TABLIST).ariaLabel(ariaLabel);
      var root = custom("tessera-tabs").child(tabBar);

      for (var i = 0; i < panels.size(); i++) {
        var tabRef = ElementRef.named("tab-" + i);
        var panelRef = ElementRef.named("panel-" + i);
        tabBar.child(
            button()
                .id(tabRef)
                .role(Role.TAB)
                .ariaSelected(i == 0)
                .ariaControls(panelRef)
                .tabIndex(i == 0 ? 0 : -1)
                .text(panels.get(i).label()));
        root.child(
            div()
                .id(panelRef)
                .role(Role.TABPANEL)
                .ariaLabelledBy(tabRef)
                .tabIndex(0)
                .boolAttr("hidden", i != 0)
                .child(panels.get(i).content()));
      }
      return root;
    }
  }

  /// Anchor-positioned underline sliding to the tab carrying `anchorIdent`.
  private static ScopeStyle anchorUnderline(
      ScopeStyle scope, String barSelector, CssIdent anchorIdent) {
    return scope
        .select(
            barSelector,
            s ->
                s.content(CssContent.empty())
                    .absolute()
                    .positionAnchor(anchorIdent)
                    .insetBlockStart(
                        CssLength.anchor(CssLength.AnchorSide.BOTTOM).minus(CssLength.px(2)))
                    .insetInlineStart(CssLength.anchor(CssLength.AnchorSide.LEFT))
                    .insetInlineEnd(CssLength.anchor(CssLength.AnchorSide.RIGHT))
                    .blockSize(CssLength.px(2))
                    .bg(Tokens.ACCENT_PRIMARY))
        .motionSafe(
            m ->
                m.select(
                    barSelector,
                    s ->
                        s.motion(Motion.of(CssDuration.ms(200), Easing.ease(), Animatable.INSET))));
  }
}
