package dev.nthings.tessera.internal.components;

import dev.nthings.tessera.components.ContentTabs;
import dev.nthings.tessera.components.Drawer;
import dev.nthings.tessera.components.Field;
import dev.nthings.tessera.components.Field.Validation;
import dev.nthings.tessera.components.Image;
import dev.nthings.tessera.components.Modal;
import dev.nthings.tessera.components.SearchPalette;
import dev.nthings.tessera.components.Section;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.Scope;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.core.ViewContext;
import dev.nthings.tessera.internal.authoring.AuthoringAccess;
import dev.nthings.tessera.internal.authoring.IdReference;
import dev.nthings.tessera.internal.authoring.PortableNode;
import dev.nthings.tessera.internal.foundation.Registration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/// Internal bridge exposing portable component internals to document decorators and the built-in
/// browser widget adapters without creating package cycles or public registration hooks. Each
/// adapter receives its component's host together with a typed model of the parts it drives, bound
/// to [ElementRef]s, so a browser controller never rediscovers structure from class names,
/// attributes or child order.
public final class WidgetBridge {
  private static final Registration<TabsRenderer> TABS = new Registration<>("Tabs widget accessor");
  private static final Registration<BiFunction<Field, @Nullable Validation, FieldParts>> FIELDS =
      new Registration<>("Field widget accessor");
  private static final Registration<BiFunction<SearchPalette, ViewContext, SearchParts>> SEARCH =
      new Registration<>("SearchPalette widget accessor");
  private static final Registration<BiFunction<IntoElement, ViewContext, DialogParts>> DIALOGS =
      new Registration<>("Dialog widget accessor");
  private static final Registration<Function<Image, ImageParts>> IMAGES =
      new Registration<>("Image accessor");
  private static final Registration<Function<Section, SectionParts>> SECTIONS =
      new Registration<>("Section accessor");

  private WidgetBridge() {}

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

  public record ImageParts(String src, TagElement eagerElement) {}

  public record SectionParts(TagElement section, TagElement body) {}

  public static void registerTabs(TabsRenderer renderer) {
    TABS.register(renderer);
  }

  public static TabsParts tabs(
      ContentTabs source, ViewContext context, boolean controlled, @Nullable String selection) {
    return TABS.get().render(source, context, controlled, selection);
  }

  public static void registerField(BiFunction<Field, @Nullable Validation, FieldParts> renderer) {
    FIELDS.register(renderer);
  }

  public static FieldParts field(Field source, @Nullable Validation validation) {
    return FIELDS.get().apply(source, validation);
  }

  /// The scope of a portable built-in scope such as a dialog, field, tabs or search palette.
  public static Scope scope(IntoElement source) {
    return ((PortableNode.ScopePlacement) AuthoringAccess.portable(source.intoElement())).scope();
  }

  /// Registers the renderer shared by [Modal] and [Drawer]; it receives only those types.
  public static void registerDialog(BiFunction<IntoElement, ViewContext, DialogParts> renderer) {
    DIALOGS.register(renderer);
  }

  public static DialogParts dialog(Modal source, ViewContext context) {
    return DIALOGS.get().apply(source, context);
  }

  public static DialogParts dialog(Drawer source, ViewContext context) {
    return DIALOGS.get().apply(source, context);
  }

  public static void registerSearch(BiFunction<SearchPalette, ViewContext, SearchParts> renderer) {
    SEARCH.register(renderer);
  }

  public static SearchParts search(SearchPalette palette, ViewContext context) {
    return SEARCH.get().apply(palette, context);
  }

  public static void registerImage(Function<Image, ImageParts> renderer) {
    IMAGES.register(renderer);
  }

  public static ImageParts image(Image image) {
    return IMAGES.get().apply(image);
  }

  public static void registerSection(Function<Section, SectionParts> renderer) {
    SECTIONS.register(renderer);
  }

  public static SectionParts section(Section section) {
    return SECTIONS.get().apply(section);
  }

  /// Splits a trigger configured with `interestTooltip` into a copy of the trigger, anchored and
  /// described by the popup, and the popup anchored to it.
  public static TooltipParts tooltip(TagElement source) {
    var trigger = source.copy().anchor();
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
