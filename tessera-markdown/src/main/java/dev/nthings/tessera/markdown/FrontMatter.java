package dev.nthings.tessera.markdown;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/// The YAML front matter of a Markdown page, between `---` lines at the start of the source.
///
/// [#values()] holds what the built-in parser reads: top-level keys with a scalar or a list of
/// scalars, as strings. Nested maps, numbers, dates, and booleans stay text. For anything richer,
/// parse [#yaml()] with a YAML library.
///
/// @param yaml the raw YAML between the separators, or an empty string without front matter
/// @param values each key's values, in source order
public record FrontMatter(String yaml, Map<String, List<String>> values) {

  /// Front matter of a page that has none.
  public static final FrontMatter NONE = new FrontMatter("", Map.of());

  public FrontMatter {
    var copy = new LinkedHashMap<String, List<String>>();
    values.forEach((key, list) -> copy.put(key, List.copyOf(list)));
    values = Collections.unmodifiableMap(copy);
  }

  /// The first value of `key`, or empty when the key is absent or has no value.
  public Optional<String> value(String key) {
    var list = values.get(key);
    return list == null || list.isEmpty() ? Optional.empty() : Optional.of(list.getFirst());
  }

  /// Every value of `key`, such as the items of a `tags` list, or an empty list.
  public List<String> list(String key) {
    return values.getOrDefault(key, List.of());
  }
}
