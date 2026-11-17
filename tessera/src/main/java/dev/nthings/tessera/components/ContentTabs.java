package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.button;
import static dev.nthings.tessera.core.Elements.custom;
import static dev.nthings.tessera.core.Elements.div;

import dev.nthings.tessera.core.DocumentContext;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.PageScript;
import dev.nthings.tessera.core.Role;
import dev.nthings.tessera.css.Anchor;
import dev.nthings.tessera.css.Border;
import dev.nthings.tessera.css.CssColor;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.Cursor;
import dev.nthings.tessera.css.Overflow;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.State;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.WhiteSpace;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/// In-page tabs that switch between panels of content, following the ARIA tabs pattern. Create one
/// with [#of(String)] and add tabs with [#tab(String, String, IntoElement)]; the first tab starts
/// selected. All panels are in the page. Arrow, Home and End keys move between tabs, which HTML
/// does not do on its own, so the component contributes the `tessera-tabs` page script. Until that
/// script loads, the tab bar is hidden and every panel is visible. Tabs without any tab report
/// `empty-content-tabs`. Two tabs with the same key report `duplicate-tab`.
///
/// Each tab's key becomes the id suffix, `<id>--tab-<key>` and `<id>--panel-<key>`, so inserting a
/// tab does not rename the others. Characters outside `[A-Za-z0-9_-]` become `-`. Without an
/// [#id(String)], the compiler numbers instances `content-tabs-<n>`.
public final class ContentTabs extends BuiltinScope<ContentTabs> {

  private static final PageScript TABS_SCRIPT =
      PageScript.fromResource("tessera-tabs", ContentTabs.class, "tessera-tabs.js");

  /// The selected tab's anchor; each bar's underline is positioned inside its own tab bar.
  private static final Anchor ACTIVE = Anchor.named("tessera-active-tab");

  private record Panel(String key, String label, Element content) {}

  private final String label;
  private final List<Panel> panels = new ArrayList<>();

  private ContentTabs(String label) {
    this.label = label;
  }

  /// Creates empty tabs whose tab list is named `label`.
  public static ContentTabs of(String label) {
    return new ContentTabs(label);
  }

  /// Adds a tab with the stable id suffix `key`, showing `label`, whose panel holds `content`.
  /// Throws IllegalArgumentException if `key` is blank.
  public ContentTabs tab(String key, String label, IntoElement content) {
    if (key.isBlank()) {
      throw new IllegalArgumentException("A tab key must not be blank");
    }
    panels.add(new Panel(key, label, content.intoElement()));
    return this;
  }

  @Override
  public ContentTabs self() {
    return this;
  }

  @Override
  String scopeName() {
    return "content-tabs";
  }

  @Override
  void configureScope(ScopeStyle scope) {
    scope
        .root(s ->
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
                    .cursor(Cursor.POINTER)
                    .on(State.HOVER, h -> h.bg(Tokens.SURFACE_CARD).textColor(Tokens.TEXT_PRIMARY))
                    .on(State.FOCUS_VISIBLE, f -> f.focusRing(Tokens.ACCENT_PRIMARY)))
        .select(
            ":scope > .tab-bar > [role=\"tab\"][aria-selected=\"true\"]",
            s -> s.anchorName(ACTIVE).textColor(Tokens.ACCENT_PRIMARY).fontWeight(600))
        .select(":scope > [role=\"tabpanel\"]", s -> s.p(Tokens.SPACE_5))
        // Before `tessera-tabs` upgrades, show every panel rather than hiding all but the first.
        .select(":scope:not(:defined) > .tab-bar", s -> s.hidden())
        .select(":scope:not(:defined) > [role=\"tabpanel\"][hidden]", s -> s.block());
    TabUnderline.apply(scope, ":scope > .tab-bar::before", ACTIVE);
  }

  @Override
  IntoElement expand(DocumentContext context) {
    context.contributeScript(TABS_SCRIPT);
    if (panels.isEmpty()) {
      context.report(
          "empty-content-tabs",
          "ContentTabs '" + label + "' has no tabs",
          "Add at least one tab with tab(key, label, content).");
    }

    var tabBar = div().attr("class", "tab-bar").role(Role.TABLIST).ariaLabel(label);
    var root = custom("tessera-tabs").child(tabBar);
    var seen = new HashSet<String>();

    for (var i = 0; i < panels.size(); i++) {
      var panel = panels.get(i);
      if (!seen.add(panel.key())) {
        context.report(
            "duplicate-tab",
            "Duplicate tab key '" + panel.key() + "'",
            "Give each tab of this ContentTabs a distinct key.");
      }
      var tabRef = ElementRef.scoped("tab-" + panel.key());
      var panelRef = ElementRef.scoped("panel-" + panel.key());
      tabBar.child(
          button()
              .id(tabRef)
              .role(Role.TAB)
              .ariaSelected(i == 0)
              .ariaControls(panelRef)
              .tabIndex(i == 0 ? 0 : -1)
              .text(panel.label()));
      root.child(
          div()
              .id(panelRef)
              .role(Role.TABPANEL)
              .ariaLabelledBy(tabRef)
              .tabIndex(0)
              .onlyIf(i != 0, el -> el.boolAttr("hidden"))
              .child(panel.content()));
    }
    return root;
  }
}
