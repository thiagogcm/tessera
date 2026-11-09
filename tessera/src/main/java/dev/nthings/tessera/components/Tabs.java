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
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.RenderContext;
import dev.nthings.tessera.core.Role;
import dev.nthings.tessera.core.View;
import dev.nthings.tessera.css.Anchor;
import dev.nthings.tessera.css.Animatable;
import dev.nthings.tessera.css.Border;
import dev.nthings.tessera.css.CssColor;
import dev.nthings.tessera.css.CssContent;
import dev.nthings.tessera.css.CssDuration;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.Cursor;
import dev.nthings.tessera.css.Easing;
import dev.nthings.tessera.css.Media;
import dev.nthings.tessera.css.Overflow;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.Transition;
import dev.nthings.tessera.css.WhiteSpace;
import dev.nthings.tessera.foundation.Fnv1a64;
import java.util.List;

/// Tab bars for site navigation ([#navTabs]) and for switching between panels of content
/// ([#contentTabs]). Both mark the active tab with an underline that slides between tabs.
public final class Tabs {

  private Tabs() {}

  /// Link in a [Tabs#navTabs] bar: the `href` it points to and the `label` shown.
  public record NavItem(String href, String label) {}

  /// Panel of a [Tabs#contentTabs(String, List)] set: `label` names its tab and `content` fills
  /// the panel.
  public record TabPanel(String label, Element content) {
    /// Creates a panel whose content may contain markup.
    public static TabPanel of(String label, IntoElement content) {
      return new TabPanel(label, content.intoElement());
    }
  }

  /// Creates a navigation bar of links. The item whose `href` equals `activeHref` is marked
  /// with `aria-current="page"`; the comparison is exact, so `/guide` and `/guide/` differ.
  /// `ariaLabel` names the `<nav>`.
  public static NavTabs navTabs(
      String id, String ariaLabel, String activeHref, List<NavItem> items) {
    return new NavTabs(id, ariaLabel, activeHref, items);
  }

  /// Creates in-page tabs whose tab list is named "Content tabs". Prefer
  /// [#contentTabs(String, String, List)] to give the tab list a specific name.
  public static ContentTabs contentTabs(String id, List<TabPanel> panels) {
    return contentTabs(id, "Content tabs", panels);
  }

  /// Creates in-page tabs whose tab list is named `ariaLabel`. The first panel starts selected.
  /// Throws IllegalArgumentException if `panels` is empty.
  public static ContentTabs contentTabs(String id, String ariaLabel, List<TabPanel> panels) {
    return new ContentTabs(id, ariaLabel, panels);
  }

  /// Navigation bar of links, created with [Tabs#navTabs]. The active link is marked with
  /// `aria-current="page"`.
  public static final class NavTabs implements View {

    private final String id;
    private final String ariaLabel;
    private final Anchor anchorIdent;
    private final String activeHref;
    private final List<NavItem> items;

    public NavTabs(String id, String ariaLabel, String activeHref, List<NavItem> items) {
      this.id = Fnv1a64.sanitizeLocalId(id);
      this.ariaLabel = ariaLabel;
      this.anchorIdent = Anchor.named("active-nav-" + this.id);
      this.activeHref = activeHref;
      this.items = List.copyOf(items);
    }

    @Override
    public String viewId() {
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

  /// In-page tabs, created with [Tabs#contentTabs(String, List)] or its labelled overload. All
  /// panels
  /// are in the page; the tab behavior shows the selected one, and arrow, Home and End keys move
  /// between tabs. Until the behavior loads, the tab bar is hidden and every panel is visible.
  public static final class ContentTabs implements View {

    private static final ClientBehavior TABS_BEHAVIOR =
        ClientBehavior.fromResource("tessera-tabs", ContentTabs.class, "tessera-tabs.js");

    private final String id;
    private final String ariaLabel;
    private final Anchor anchorIdent;
    private final List<TabPanel> panels;

    public ContentTabs(String id, String ariaLabel, List<TabPanel> panels) {
      if (panels.isEmpty()) {
        throw new IllegalArgumentException("ContentTabs requires at least 1 TabPanel");
      }
      this.id = Fnv1a64.sanitizeLocalId(id);
      this.ariaLabel = ariaLabel;
      this.anchorIdent = Anchor.named("active-tab-" + this.id);
      this.panels = List.copyOf(panels);
    }

    @Override
    public String viewId() {
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
                      .border(Border.hairline(Tokens.BORDER_SUBTLE))
                      .overflow(Overflow.CLIP))
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
                      .borderBlockEnd(Border.hairline(Tokens.BORDER_SUBTLE))
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
                      .border(Border.none())
                      .textColor(Tokens.TEXT_MUTED)
                      .fontFamilyInherit()
                      .fontSize(Tokens.FONT_SIZE_SM)
                      .fontWeight(500)
                      .whiteSpace(WhiteSpace.NOWRAP)
                      .cursor(Cursor.POINTER))
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
        var tabRef = ElementRef.scoped("tab-" + i);
        var panelRef = ElementRef.scoped("panel-" + i);
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
      ScopeStyle scope, String barSelector, Anchor anchorIdent) {
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
        .on(
            Media.MOTION_SAFE,
            m ->
                m.select(
                    barSelector,
                    s ->
                        s.transition(
                            Transition.of(CssDuration.ms(200), Easing.ease(), Animatable.INSET))));
  }
}
