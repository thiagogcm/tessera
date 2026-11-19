package dev.nthings.tessera.internal.authoring;

import dev.nthings.tessera.core.Component;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.FragmentElement;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.core.ViewKey;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/// Access to builder data across packages in this module. Each builder installs its accessor during
/// class initialization, before an instance can be passed here.
public final class AuthoringAccess {
  private static final AtomicReference<@Nullable Function<TagElement, TagData>> TAGS =
      new AtomicReference<>();
  private static final AtomicReference<@Nullable Function<FragmentElement, List<Element>>>
      FRAGMENTS = new AtomicReference<>();
  private static final AtomicReference<@Nullable Function<Element, PortableNode>> PORTABLE_UNWRAP =
      new AtomicReference<>();
  private static final AtomicReference<@Nullable Function<PortableNode, Element>> PORTABLE_WRAP =
      new AtomicReference<>();
  private static final AtomicReference<@Nullable Function<ElementRef, Ref>> REFERENCES =
      new AtomicReference<>();
  private static final AtomicReference<@Nullable StateAccess> STATE = new AtomicReference<>();

  public record Ref(@Nullable String name, boolean pageLevel) {}

  public interface StateAccess {
    <T> T getOrCreate(ViewKey<T> key, Map<ViewKey<?>, Object> values);
  }

  private AuthoringAccess() {}

  public static void registerRef(Function<ElementRef, Ref> accessor) {
    if (!REFERENCES.compareAndSet(null, accessor)) {
      throw new IllegalStateException("ElementRef accessor already registered");
    }
  }

  public static Ref reference(ElementRef ref) {
    return Objects.requireNonNull(REFERENCES.get()).apply(ref);
  }

  public static void registerState(StateAccess accessor) {
    if (!STATE.compareAndSet(null, accessor)) {
      throw new IllegalStateException("ViewKey state accessor already registered");
    }
  }

  public static <T> T state(ViewKey<T> key, Map<ViewKey<?>, Object> values) {
    return Objects.requireNonNull(STATE.get()).getOrCreate(key, values);
  }

  public static void registerTag(Function<TagElement, TagData> accessor) {
    if (!TAGS.compareAndSet(null, accessor)) {
      throw new IllegalStateException("TagElement accessor already registered");
    }
  }

  public static TagData tag(TagElement element) {
    return Objects.requireNonNull(TAGS.get()).apply(element);
  }

  public static void registerFragment(Function<FragmentElement, List<Element>> accessor) {
    if (!FRAGMENTS.compareAndSet(null, accessor)) {
      throw new IllegalStateException("FragmentElement accessor already registered");
    }
  }

  public static List<Element> fragment(FragmentElement element) {
    return Objects.requireNonNull(FRAGMENTS.get()).apply(element);
  }

  public static void registerPortable(
      Function<Element, PortableNode> unwrap, Function<PortableNode, Element> wrap) {
    if (!PORTABLE_UNWRAP.compareAndSet(null, unwrap) || !PORTABLE_WRAP.compareAndSet(null, wrap)) {
      throw new IllegalStateException("Portable placement accessor already registered");
    }
  }

  public static PortableNode portable(Element placement) {
    return Objects.requireNonNull(PORTABLE_UNWRAP.get()).apply(placement);
  }

  public static Element component(Class<?> type, Component component) {
    return Holder.WRAP.apply(
        new PortableNode.ComponentPlacement(component, type, SourceProvenance.capture()));
  }

  private static final class Holder {
    static final Function<PortableNode, Element> WRAP = load();

    private static Function<PortableNode, Element> load() {
      // Trigger PortablePlacement class initialization on first wrap.
      var seed = Elements.empty();
      return Objects.requireNonNull(
          PORTABLE_WRAP.get(),
          () -> "No portable placement accessor registered for " + seed.getClass().getSimpleName());
    }
  }
}
