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
import dev.nthings.tessera.internal.authoring.PortableNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import org.jspecify.annotations.Nullable;

/// Shared authoring paths for the built-in browser widget adapters. Each adapter receives its
/// component's host together with a typed model of the parts it drives, bound to [ElementRef]s, so
/// a browser controller never rediscovers structure from class names, attributes or child order.
public final class WidgetAccess {
  private static @Nullable TabsRenderer tabs;
  private static @Nullable BiFunction<Field, @Nullable Validation, FieldParts> fields;
  private static @Nullable BiFunction<SearchPalette, ViewContext, SearchParts> search;
  private static @Nullable BiFunction<IntoElement, ViewContext, DialogParts> dialogs;

  private WidgetAccess() {}

  @FunctionalInterface
  public interface TabsRenderer {
    TabsParts render(
        ContentTabs tabs, ViewContext context, boolean controlled, @Nullable String selection);
  }

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

  public static void registerTabs(TabsRenderer renderer) {
    tabs = renderer;
  }

  public static TabsParts tabs(
      ContentTabs source, ViewContext context, boolean controlled, @Nullable String selection) {
    var parts = Objects.requireNonNull(tabs).render(source, context, controlled, selection);
    supplyBehavior(parts.host());
    return parts;
  }

  public static void registerField(BiFunction<Field, @Nullable Validation, FieldParts> renderer) {
    fields = renderer;
  }

  public static FieldParts field(Field source, @Nullable Validation validation) {
    return Objects.requireNonNull(fields).apply(source, validation);
  }

  public static Scope scope(IntoElement source) {
    return ((PortableNode.ScopePlacement) source.intoElement()).scope();
  }

  public static void registerDialog(BiFunction<IntoElement, ViewContext, DialogParts> renderer) {
    dialogs = renderer;
  }

  /// The parts of a portable `Modal` or `Drawer`.
  public static DialogParts dialog(IntoElement source, ViewContext context) {
    return Objects.requireNonNull(dialogs).apply(source, context);
  }

  public static void registerSearch(BiFunction<SearchPalette, ViewContext, SearchParts> renderer) {
    search = renderer;
  }

  public static SearchParts search(SearchPalette palette, ViewContext context) {
    var parts = Objects.requireNonNull(search).apply(palette, context);
    supplyBehavior(parts.host());
    return parts;
  }

  /// Clears the bundled behavior of a host whose adapter supplies it, so the host renders as its
  /// plain element instead of an undefined custom element.
  private static void supplyBehavior(TagElement host) {
    AuthoringAccess.tag(host).behavior = null;
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
