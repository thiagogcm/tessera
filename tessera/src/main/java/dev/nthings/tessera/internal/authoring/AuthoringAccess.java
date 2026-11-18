package dev.nthings.tessera.internal.authoring;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.FragmentElement;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.core.ViewKey;
import dev.nthings.tessera.document.DocumentElement;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/// Access to builder data across packages in this module. Each builder installs its accessor during
/// class initialization, before an instance can be passed here.
public final class AuthoringAccess {
  private static @Nullable Function<TagElement, TagData> tags;
  private static @Nullable Function<FragmentElement, List<Element>> fragments;
  private static @Nullable Function<DocumentElement, DocumentNode.Host> documentHosts;

  private static @Nullable Function<ElementRef, Ref> references;
  private static @Nullable StateAccess state;

  public record Ref(@Nullable String name, boolean pageLevel) {}

  public interface StateAccess {
    <T> T getOrCreate(ViewKey<T> key, Map<ViewKey<?>, Object> values);
  }

  private AuthoringAccess() {}

  public static void registerRef(Function<ElementRef, Ref> accessor) {
    references = accessor;
  }

  public static Ref reference(ElementRef ref) {
    return Objects.requireNonNull(references).apply(ref);
  }

  public static void registerState(StateAccess accessor) {
    state = accessor;
  }

  public static <T> T state(ViewKey<T> key, Map<ViewKey<?>, Object> values) {
    return Objects.requireNonNull(state).getOrCreate(key, values);
  }

  public static void registerTag(Function<TagElement, TagData> accessor) {
    tags = accessor;
  }

  public static TagData tag(TagElement element) {
    return Objects.requireNonNull(tags).apply(element);
  }

  public static void registerFragment(Function<FragmentElement, List<Element>> accessor) {
    fragments = accessor;
  }

  public static List<Element> fragment(FragmentElement element) {
    return Objects.requireNonNull(fragments).apply(element);
  }

  public static void registerDocumentHost(Function<DocumentElement, DocumentNode.Host> accessor) {
    documentHosts = accessor;
  }

  public static DocumentNode.Host documentHost(DocumentElement element) {
    return Objects.requireNonNull(documentHosts).apply(element);
  }
}
