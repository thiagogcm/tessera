package dev.nthings.tessera.internal.authoring;

import dev.nthings.tessera.core.Component;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.FragmentElement;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.core.ViewKey;
import dev.nthings.tessera.internal.foundation.Registration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/// Access to builder data across packages in this module. Each builder installs its accessor during
/// class initialization, before an instance can be passed here.
public final class AuthoringAccess {
  private static final Registration<Function<TagElement, TagData>> TAGS =
      new Registration<>("TagElement accessor");
  private static final Registration<Function<FragmentElement, List<Element>>> FRAGMENTS =
      new Registration<>("FragmentElement accessor");
  private static final Registration<Function<Element, PortableNode>> PORTABLE_UNWRAP =
      new Registration<>("Portable placement accessor");
  // Wrapping is the first use of a portable placement, so it initializes PortablePlacement.
  private static final Registration<Function<PortableNode, Element>> PORTABLE_WRAP =
      new Registration<>("Portable placement accessor", Elements::empty);
  private static final Registration<Function<ElementRef, Ref>> REFERENCES =
      new Registration<>("ElementRef accessor");
  private static final Registration<StateAccess> STATE =
      new Registration<>("ViewKey state accessor");

  public record Ref(@Nullable String name, boolean pageLevel) {}

  public interface StateAccess {
    <T> T getOrCreate(ViewKey<T> key, Map<ViewKey<?>, Object> values);
  }

  private AuthoringAccess() {}

  public static void registerRef(Function<ElementRef, Ref> accessor) {
    REFERENCES.register(accessor);
  }

  public static Ref reference(ElementRef ref) {
    return REFERENCES.get().apply(ref);
  }

  public static void registerState(StateAccess accessor) {
    STATE.register(accessor);
  }

  public static <T> T state(ViewKey<T> key, Map<ViewKey<?>, Object> values) {
    return STATE.get().getOrCreate(key, values);
  }

  public static void registerTag(Function<TagElement, TagData> accessor) {
    TAGS.register(accessor);
  }

  public static TagData tag(TagElement element) {
    return TAGS.get().apply(element);
  }

  public static void registerFragment(Function<FragmentElement, List<Element>> accessor) {
    FRAGMENTS.register(accessor);
  }

  public static List<Element> fragment(FragmentElement element) {
    return FRAGMENTS.get().apply(element);
  }

  public static void registerPortable(
      Function<Element, PortableNode> unwrap, Function<PortableNode, Element> wrap) {
    PORTABLE_UNWRAP.register(unwrap);
    PORTABLE_WRAP.register(wrap);
  }

  public static PortableNode portable(Element placement) {
    return PORTABLE_UNWRAP.get().apply(placement);
  }

  public static Element component(Class<?> type, Component component) {
    return PORTABLE_WRAP
        .get()
        .apply(new PortableNode.ComponentPlacement(component, type, SourceProvenance.capture()));
  }
}
