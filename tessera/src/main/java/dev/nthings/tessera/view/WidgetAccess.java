package dev.nthings.tessera.view;

import dev.nthings.tessera.components.ContentTabs;
import dev.nthings.tessera.components.Field;
import dev.nthings.tessera.components.Field.Validation;
import dev.nthings.tessera.components.SearchPalette;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.Scope;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.core.ViewContext;
import dev.nthings.tessera.internal.authoring.AuthoringAccess;
import dev.nthings.tessera.internal.authoring.IdReference;
import dev.nthings.tessera.internal.components.WidgetBridge;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/// Read-only authoring bridge for the built-in browser widget adapters. Each adapter receives its
/// component's host together with a typed model of the parts it drives, bound to [ElementRef]s, so
/// a browser controller never rediscovers structure from class names, attributes or child order.
public final class WidgetAccess {

  private WidgetAccess() {}

  public record FieldParts(
      TagElement host,
      List<TagElement> before,
      TagElement control,
      @Nullable Scope controlScope,
      List<TagElement> after) {}

  public record TooltipParts(TagElement trigger, TagElement popup, ElementRef popupRef) {}

  /// Keyed tabs: the host, each tab in order, the selection the host renders, or `null` when no tab
  /// is enabled, and whether arrow keys only move focus.
  public record TabsParts(
      TagElement host, List<TabPart> tabs, @Nullable String selected, boolean manual) {}

  /// One tab: its key, whether it can be selected, and the refs bound to its tab and panel.
  public record TabPart(String key, boolean enabled, ElementRef tab, ElementRef panel) {}

  /// A search palette's dialog, its query input, its no-results status and its results in order.
  public record SearchParts(
      TagElement host, ElementRef input, ElementRef empty, List<SearchResult> results) {}

  /// One result link, bound to `ref`, and what it shows.
  public record SearchResult(ElementRef ref, SearchLink link) {}

  /// What a result shows, compared by value: a result that changes position keeps its identity.
  public record SearchLink(
      String group,
      String title,
      String href,
      @Nullable String description,
      @Nullable String badge) {}

  /// A dialog's host without `open`, and whether the portable dialog was authored open.
  public record DialogParts(TagElement host, boolean opened) {}

  public static TabsParts tabs(
      ContentTabs source, ViewContext context, boolean controlled, @Nullable String selection) {
    var raw = WidgetBridge.tabs(source, context, controlled, selection);
    return new TabsParts(
        raw.host(),
        raw.tabs().stream()
            .map(t -> new TabPart(t.key(), t.enabled(), t.tab(), t.panel()))
            .toList(),
        raw.selected(),
        raw.manual());
  }

  public static FieldParts field(Field source, @Nullable Validation validation) {
    var raw = WidgetBridge.field(source, validation);
    return new FieldParts(raw.host(), raw.before(), raw.control(), raw.controlScope(), raw.after());
  }

  public static Scope scope(IntoElement source) {
    return WidgetBridge.scope(source);
  }

  /// The parts of a portable `Modal` or `Drawer`.
  public static DialogParts dialog(IntoElement source, ViewContext context) {
    var raw = WidgetBridge.dialog(source, context);
    return new DialogParts(raw.host(), raw.opened());
  }

  public static SearchParts search(SearchPalette palette, ViewContext context) {
    var raw = WidgetBridge.search(palette, context);
    return new SearchParts(
        raw.host(),
        raw.input(),
        raw.empty(),
        raw.results().stream()
            .map(r ->
                new SearchResult(
                    r.ref(),
                    new SearchLink(
                        r.link().group(),
                        r.link().title(),
                        r.link().href(),
                        r.link().description(),
                        r.link().badge())))
            .toList());
  }

  public static TooltipParts tooltip(TagElement source) {
    var trigger = source.copy().anchorName();
    var data = AuthoringAccess.tag(trigger);
    var popup =
        Objects.requireNonNull(
            data.interestTooltip, "Configure the trigger with interestTooltip before adapting it");
    data.interestTooltip = null;
    var popupRef = popup.ref();
    AuthoringAccess.tag(popup).anchorTarget = trigger.ref();
    var description = data.idReferences.get("aria-describedby");
    var targets = new ArrayList<ElementRef>();
    if (description != null) {
      targets.addAll(description.targets());
    }
    if (!targets.contains(popupRef)) {
      targets.add(popupRef);
    }
    data.idReferences.put("aria-describedby", new IdReference("", targets));
    return new TooltipParts(trigger, popup, popupRef);
  }
}
