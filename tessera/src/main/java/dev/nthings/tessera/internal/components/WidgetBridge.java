package dev.nthings.tessera.internal.components;

import dev.nthings.tessera.components.ContentTabs;
import dev.nthings.tessera.components.Field;
import dev.nthings.tessera.components.Field.Validation;
import dev.nthings.tessera.components.Image;
import dev.nthings.tessera.components.SearchPalette;
import dev.nthings.tessera.components.Section;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.Scope;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.core.ViewContext;
import dev.nthings.tessera.internal.authoring.AuthoringAccess;
import dev.nthings.tessera.internal.authoring.PortableNode;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/// Internal bridge exposing portable component internals to document decorators and the qualified
/// browser widget facade without creating package cycles or public registration hooks.
public final class WidgetBridge {
  private static final AtomicReference<@Nullable TabsRenderer> TABS = new AtomicReference<>();
  private static final AtomicReference<
          @Nullable BiFunction<Field, @Nullable Validation, FieldParts>>
      FIELDS = new AtomicReference<>();
  private static final AtomicReference<
          @Nullable BiFunction<SearchPalette, ViewContext, SearchParts>>
      SEARCH = new AtomicReference<>();
  private static final AtomicReference<@Nullable BiFunction<IntoElement, ViewContext, DialogParts>>
      DIALOGS = new AtomicReference<>();
  private static final AtomicReference<@Nullable Function<Image, ImageParts>> IMAGES =
      new AtomicReference<>();
  private static final AtomicReference<@Nullable Function<Section, SectionParts>> SECTIONS =
      new AtomicReference<>();

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

  public record TabsParts(
      TagElement host, List<TabPart> tabs, @Nullable String selected, boolean manual) {}

  public record TabPart(String key, boolean enabled, ElementRef tab, ElementRef panel) {}

  public record SearchParts(
      TagElement host, ElementRef input, ElementRef empty, List<SearchResult> results) {}

  public record SearchResult(ElementRef ref, SearchLink link) {}

  public record SearchLink(
      String group,
      String title,
      String href,
      @Nullable String description,
      @Nullable String badge) {}

  public record DialogParts(TagElement host, boolean opened) {}

  public record ImageParts(String src, TagElement eagerElement) {}

  public record SectionParts(TagElement section, TagElement body) {}

  public static void registerTabs(TabsRenderer renderer) {
    if (!TABS.compareAndSet(null, renderer)) {
      throw new IllegalStateException("Tabs widget accessor already registered");
    }
  }

  public static TabsParts tabs(
      ContentTabs source, ViewContext context, boolean controlled, @Nullable String selection) {
    return Objects.requireNonNull(TABS.get()).render(source, context, controlled, selection);
  }

  public static void registerField(BiFunction<Field, @Nullable Validation, FieldParts> renderer) {
    if (!FIELDS.compareAndSet(null, renderer)) {
      throw new IllegalStateException("Field widget accessor already registered");
    }
  }

  public static FieldParts field(Field source, @Nullable Validation validation) {
    return Objects.requireNonNull(FIELDS.get()).apply(source, validation);
  }

  public static Scope scope(IntoElement source) {
    return ((PortableNode.ScopePlacement) AuthoringAccess.portable(source.intoElement())).scope();
  }

  public static void registerDialog(BiFunction<IntoElement, ViewContext, DialogParts> renderer) {
    if (!DIALOGS.compareAndSet(null, renderer)) {
      throw new IllegalStateException("Dialog widget accessor already registered");
    }
  }

  public static DialogParts dialog(IntoElement source, ViewContext context) {
    var renderer = DIALOGS.get();
    if (renderer == null) {
      throw new IllegalArgumentException(
          "Expected a Modal or Drawer dialog, got " + source.getClass().getSimpleName());
    }
    return renderer.apply(source, context);
  }

  public static void registerSearch(BiFunction<SearchPalette, ViewContext, SearchParts> renderer) {
    if (!SEARCH.compareAndSet(null, renderer)) {
      throw new IllegalStateException("SearchPalette widget accessor already registered");
    }
  }

  public static SearchParts search(SearchPalette palette, ViewContext context) {
    return Objects.requireNonNull(SEARCH.get()).apply(palette, context);
  }

  public static void registerImage(Function<Image, ImageParts> renderer) {
    if (!IMAGES.compareAndSet(null, renderer)) {
      throw new IllegalStateException("Image accessor already registered");
    }
  }

  public static ImageParts image(Image image) {
    return Objects.requireNonNull(IMAGES.get()).apply(image);
  }

  public static void registerSection(Function<Section, SectionParts> renderer) {
    if (!SECTIONS.compareAndSet(null, renderer)) {
      throw new IllegalStateException("Section accessor already registered");
    }
  }

  public static SectionParts section(Section section) {
    return Objects.requireNonNull(SECTIONS.get()).apply(section);
  }
}
