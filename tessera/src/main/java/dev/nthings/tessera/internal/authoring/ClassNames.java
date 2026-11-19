package dev.nthings.tessera.internal.authoring;

import dev.nthings.tessera.internal.foundation.Fnv1a64;

/// Compiler-facing: the class-derived names of components and scopes, computed once per class. A
/// class has no stable name when it is anonymous, or a lambda or other hidden or synthetic class:
/// its name can change between builds.
public final class ClassNames {

  private record Names(boolean unstable, String simpleName, String kebabName) {}

  private static final ClassValue<Names> NAMES =
      new ClassValue<>() {
        @Override
        protected Names computeValue(Class<?> type) {
          var unstable = type.isAnonymousClass() || type.isHidden() || type.isSynthetic();
          var simpleName = type.getSimpleName();
          return new Names(unstable, simpleName, Fnv1a64.toKebabCase(simpleName));
        }
      };

  private ClassNames() {}

  /// Compiler-facing: whether `type` is anonymous, hidden or synthetic, so its name can change
  /// between builds.
  public static boolean isUnstable(Class<?> type) {
    return NAMES.get(type).unstable();
  }

  /// The simple name of `type`, or `Component` for a class without a stable name.
  public static String componentName(Class<?> type) {
    var names = NAMES.get(type);
    return names.unstable() ? "Component" : names.simpleName();
  }

  /// The kebab-cased simple name of `type`, the default scopeName().
  public static String defaultScopeName(Class<?> type) {
    return isUnstable(type) ? "" : NAMES.get(type).kebabName();
  }
}
