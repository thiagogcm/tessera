package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.button;
import static dev.nthings.tessera.core.Elements.div;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.Role;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.core.ViewContext;
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
import dev.nthings.tessera.internal.authoring.AuthoringAccess;
import dev.nthings.tessera.internal.authoring.BundledBehavior;
import dev.nthings.tessera.internal.components.WidgetBridge;
import dev.nthings.tessera.internal.components.WidgetBridge.TabPart;
import dev.nthings.tessera.internal.components.WidgetBridge.TabsParts;
import dev.nthings.tessera.internal.foundation.Fnv1a64;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.jspecify.annotations.Nullable;

/// In-page tabs that switch between panels of content, following the ARIA tabs pattern. Create one
/// with [#of(String)] and add tabs with [#tab(String, String, IntoElement)]; the first enabled tab
/// starts selected unless you call [#initialSelected(String)]. All panels are in the page. Arrow,
/// Home and End keys move between tabs, which HTML does not do on its own, so a static document
/// loads the `tessera-tabs` page script. Until that script loads, and in a browser root, which does
/// not load it, the tab bar is hidden and every panel is visible. Tabs without any tab report
/// `empty-content-tabs`. Two tabs whose keys give the same id suffix report `duplicate-tab`.
///
/// Each tab's key becomes the id suffix, `<id>--tab-<key>` and `<id>--panel-<key>`, so inserting a
/// tab does not rename the others. Characters outside `[A-Za-z0-9_-]` become `-` and runs of `-`
/// collapse to one, so `c++` and `c#` collide. Without an [#id(String)], the compiler numbers
/// instances `content-tabs-<n>`.
public final class ContentTabs extends BuiltinScope<ContentTabs> {

  /// The selected tab's anchor; each bar's underline is positioned inside its own tab bar.
  private static final Anchor ACTIVE = Anchor.named("tessera-active-tab");

  static {
    WidgetBridge.registerTabs(ContentTabs::parts);
  }

  /// Whether navigating to a tab also selects it. In manual mode, Enter or Space selects it.
  public enum Activation {
    AUTOMATIC,
    MANUAL
  }

  private record Panel(String key, String label, Element content, boolean disabled) {}

  private final String label;
  private final List<Panel> panels = new ArrayList<>();
  private @Nullable String selected;
  private Activation activation = Activation.AUTOMATIC;

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
    return add(key, label, content, false);
  }

  /// Adds a disabled tab, which cannot be selected or receive roving keyboard focus.
  public ContentTabs disabledTab(String key, String label, IntoElement content) {
    return add(key, label, content, true);
  }

  /// Selects an enabled tab on first render. An absent or disabled key reports `tab-selection`.
  public ContentTabs initialSelected(String key) {
    selected = key;
    return this;
  }

  /// Sets how arrow, Home and End keys activate tabs. Defaults to automatic activation.
  public ContentTabs activation(Activation activation) {
    this.activation = activation;
    return this;
  }

  private ContentTabs add(String key, String label, IntoElement content, boolean disabled) {
    if (key.isBlank()) {
      throw new IllegalArgumentException("A tab key must not be blank");
    }
    panels.add(new Panel(key, label, content.intoElement(), disabled));
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
  TagElement expand(ViewContext context) {
    if (panels.isEmpty()) {
      context.report(
          "empty-content-tabs",
          "ContentTabs '" + label + "' has no tabs",
          "Add at least one tab with tab(key, label, content).");
    }
    if (selected != null && !enabledKeys().contains(selected)) {
      reportSelection(context);
    }
    return render(context, selected, false).host();
  }

  /// The live parts. A browser root owns the tab set over time, so it may have no tabs and a stale
  /// `initialSelected` falls back to the first enabled tab; only a bound selection must name an
  /// enabled tab.
  private TabsParts parts(ViewContext context, boolean controlled, @Nullable String selection) {
    var enabled = enabledKeys();
    if (!controlled) {
      return render(context, enabled.contains(selected) ? selected : null, true);
    }
    if (selection != null ? !enabled.contains(selection) : !enabled.isEmpty()) {
      reportSelection(context);
    }
    return render(context, selection, true);
  }

  private List<String> enabledKeys() {
    return panels.stream().filter(panel -> !panel.disabled()).map(Panel::key).toList();
  }

  private void reportSelection(ViewContext context) {
    context.report(
        "tab-selection",
        "Selection for ContentTabs '" + label + "' must name an enabled tab",
        "Supply an enabled tab key, or an empty selection when every tab is disabled or absent.");
  }

  /// Lays out the tabs with `initial` selected, or the first enabled tab when it is `null`. Keyed
  /// content is id-scoped by its key, so only a live root, which needs tabs and panels to keep
  /// their DOM across edits, keys them: documents keep the `<id>--tab-<key>` ids.
  private TabsParts render(ViewContext context, @Nullable String initial, boolean keyed) {
    var enabled = enabledKeys();
    var active = initial != null ? initial : enabled.isEmpty() ? null : enabled.getFirst();
    var selection = enabled.contains(active) ? active : null;
    var manual = activation == Activation.MANUAL;
    // The bundled behavior emits the host as `<tessera-tabs>`.
    var root = div().onlyIf(manual, el -> el.attr("data-activation", "manual"));
    AuthoringAccess.tag(root).behavior = BundledBehavior.TABS;
    var parts = new ArrayList<TabPart>(panels.size());
    if (panels.isEmpty()) {
      return new TabsParts(root, parts, selection, manual);
    }
    var tabBar = div().attr("class", "tab-bar").role(Role.TABLIST).ariaLabel(label);
    root.child(tabBar);
    var seen = new HashMap<String, String>();

    for (var panel : panels) {
      var earlier = seen.putIfAbsent(Fnv1a64.sanitizeLocalId(panel.key()), panel.key());
      if (earlier != null) {
        context.report(
            "duplicate-tab",
            earlier.equals(panel.key())
                ? "Duplicate tab key '" + panel.key() + "'"
                : "Tab keys '" + earlier + "' and '" + panel.key() + "' give the same tab id",
            "Give each tab of this ContentTabs a key that differs in letters, digits, '_' or '-'.");
      }
      var tabRef = ElementRef.scoped("tab-" + panel.key());
      var panelRef = ElementRef.scoped("panel-" + panel.key());
      var isSelected = panel.key().equals(selection);
      var tab =
          button()
              .id(tabRef)
              .role(Role.TAB)
              .ariaSelected(isSelected)
              .ariaControls(panelRef)
              .tabIndex(isSelected ? 0 : -1)
              .onlyIf(panel.disabled(), el -> el.boolAttr("disabled").ariaDisabled(true))
              .text(panel.label());
      var contents =
          div()
              .id(panelRef)
              .role(Role.TABPANEL)
              .ariaLabelledBy(tabRef)
              .tabIndex(0)
              .onlyIf(!isSelected, el -> el.boolAttr("hidden"))
              .child(panel.content());
      tabBar.child(keyed ? tab.key(panel.key()) : tab);
      root.child(keyed ? contents.key(panel.key()) : contents);
      parts.add(new TabPart(panel.key(), !panel.disabled(), tabRef, panelRef));
    }
    return new TabsParts(root, parts, selection, manual);
  }
}
