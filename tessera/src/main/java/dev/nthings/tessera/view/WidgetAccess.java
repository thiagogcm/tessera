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

/// Shared authoring paths for the built-in browser widget adapters.
public final class WidgetAccess {
  private static @Nullable TabsRenderer tabs;
  private static @Nullable BiFunction<Field, @Nullable Validation, FieldParts> fields;

  private static @Nullable BiFunction<SearchPalette, ViewContext, TagElement> search;

  private WidgetAccess() {}

  @FunctionalInterface
  public interface TabsRenderer {
    TagElement render(
        ContentTabs tabs, ViewContext context, boolean controlled, @Nullable String selection);
  }

  public record FieldParts(
      TagElement host,
      List<TagElement> before,
      TagElement control,
      @Nullable Scope controlScope,
      List<TagElement> after) {}

  public record TooltipParts(TagElement trigger, TagElement popup, ElementRef popupRef) {}

  public static void registerTabs(TabsRenderer renderer) {
    tabs = renderer;
  }

  public static TagElement tabs(
      ContentTabs source, ViewContext context, boolean controlled, @Nullable String selection) {
    return Objects.requireNonNull(tabs).render(source, context, controlled, selection);
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

  public static boolean initiallyOpen(TagElement dialog) {
    return AuthoringAccess.tag(dialog).attributes.containsKey("open");
  }

  public static void registerSearch(BiFunction<SearchPalette, ViewContext, TagElement> renderer) {
    search = renderer;
  }

  public static TagElement search(SearchPalette palette, ViewContext context) {
    return Objects.requireNonNull(search).apply(palette, context);
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
