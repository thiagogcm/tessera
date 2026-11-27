package dev.nthings.tessera.compiler;

import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.SourceLocation;
import dev.nthings.tessera.internal.authoring.SourceBoundary;
import dev.nthings.tessera.internal.authoring.SourceCapture;
import dev.nthings.tessera.internal.authoring.SourceProvenance;
import java.security.CodeSource;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/// Opt-in capture of the Java call site that builds each element.
///
/// While active, each element and component wrapper records the first calling frame that is neither
/// Tessera's nor the platform's. Compilation carries it into diagnostics and, as
/// `data-src="File.java:42"`, into the emitted HTML. Tracking costs a stack walk per element; leave
/// it off in production.
///
/// Wrap tracked authoring and compilation in [#run(boolean, Supplier)]. Component-generated
/// elements point to the line that placed the component.
public final class SourceTracking {

  private static final StackWalker WALKER =
      StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
  private static final Set<String> LIBRARY_LOCATIONS = libraryLocations();
  private static final Set<String> LIBRARY_PACKAGES =
      Set.of(
          "dev.nthings.tessera.compiler",
          "dev.nthings.tessera.components",
          "dev.nthings.tessera.core",
          "dev.nthings.tessera.css",
          "dev.nthings.tessera.document",
          "dev.nthings.tessera.document.components",
          "dev.nthings.tessera.foundation",
          "dev.nthings.tessera.internal.authoring",
          "dev.nthings.tessera.internal.components",
          "dev.nthings.tessera.internal.css",
          "dev.nthings.tessera.internal.document",
          "dev.nthings.tessera.internal.foundation",
          "dev.nthings.tessera.internal.view",
          "dev.nthings.tessera.view");
  private static final ClassValue<Boolean> LIBRARY_CLASSES =
      new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
          var location = locationOf(type);
          return location != null
              && LIBRARY_LOCATIONS.contains(location)
              && LIBRARY_PACKAGES.contains(type.getPackageName());
        }
      };
  private static final ClassValue<Boolean> COMPILER_CLASSES =
      new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
          var pkg = type.getPackage();
          return LIBRARY_CLASSES.get(type)
              && pkg != null
              && pkg.isAnnotationPresent(SourceBoundary.class);
        }
      };

  private SourceTracking() {}

  /// Runs `work` with tracking on when `enabled` holds, such as from a command-line flag, and off
  /// otherwise. With tracking on, elements `work` builds or compiles carry their call sites.
  public static <R> R run(boolean enabled, Supplier<R> work) {
    return SourceProvenance.with(enabled ? SourceTracking::capture : SourceCapture.NONE, work);
  }

  private static @Nullable SourceLocation capture() {
    return WALKER.walk(SourceTracking::authoringFrame).orElse(null);
  }

  private static Optional<SourceLocation> authoringFrame(
      java.util.stream.Stream<StackWalker.StackFrame> frames) {
    var iterator = frames.iterator();
    while (iterator.hasNext()) {
      var frame = iterator.next();
      var type = frame.getDeclaringClass();
      if (type == SourceTracking.class) {
        continue;
      }
      if (COMPILER_CLASSES.get(type)) {
        // Reached before any authoring code: a library component is expanding.
        return Optional.empty();
      }
      if (LIBRARY_CLASSES.get(type) || isPlatform(type)) {
        continue;
      }
      var file = frame.getFileName();
      return file == null
          ? Optional.empty()
          : Optional.of(new SourceLocation(file, frame.getLineNumber()));
    }
    return Optional.empty();
  }

  private static boolean isPlatform(Class<?> type) {
    var loader = type.getClassLoader();
    return loader == null || loader == ClassLoader.getPlatformClassLoader();
  }

  private static Set<String> libraryLocations() {
    var locations = new HashSet<String>();
    var compilerLocation = locationOf(SourceTracking.class);
    if (compilerLocation != null) {
      locations.add(compilerLocation);
    }
    var coreLocation = locationOf(Elements.class);
    if (coreLocation != null) {
      locations.add(coreLocation);
    }
    return Set.copyOf(locations);
  }

  private static @Nullable String locationOf(Class<?> type) {
    CodeSource source = type.getProtectionDomain().getCodeSource();
    var location = source == null ? null : source.getLocation();
    return location == null ? null : location.toString();
  }
}
