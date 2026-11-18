package dev.nthings.tessera.view;

import dev.nthings.tessera.core.Location;
import dev.nthings.tessera.core.TagName;
import dev.nthings.tessera.internal.authoring.InitialControl;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/// The parts of an element that linking passes through unchanged, shared by the expanded
/// [ViewNode.Tag] and the linked [ViewSnapshot.Tag] it becomes.
record TagCore<M>(
    TagName tag,
    Location location,
    @Nullable M payload,
    Map<String, String> liveProperties,
    InitialControl initial) {

  TagCore {
    liveProperties = readOnly(liveProperties);
  }

  /// `map` as a read-only view, without wrapping an empty or already wrapped map.
  static <K, V> Map<K, V> readOnly(Map<K, V> map) {
    return map.isEmpty() ? Map.of() : Collections.unmodifiableMap(map);
  }

  /// `list` as a read-only view, without wrapping an empty or already wrapped list.
  static <T> List<T> readOnly(List<T> list) {
    return list.isEmpty() ? List.of() : Collections.unmodifiableList(list);
  }

  /// `set` as a read-only view, without wrapping an empty or already wrapped set.
  static <T> Set<T> readOnly(Set<T> set) {
    return set.isEmpty() ? Set.of() : Collections.unmodifiableSet(set);
  }
}
