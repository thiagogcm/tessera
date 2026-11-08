package dev.nthings.tessera.render;

import dev.nthings.tessera.core.SourceBoundary;
import dev.nthings.tessera.core.SourceCapture;
import dev.nthings.tessera.core.SourceLocation;
import dev.nthings.tessera.core.SourceProvenance;
import java.security.CodeSource;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/// Opt-in capture of the Java call site that builds each element.
///
/// While active, each element and component wrapper records the first calling frame
/// code that is neither Tessera's own nor the platform's. Compilation carries it into diagnostics
/// and, as `data-src="File.java:42"`, into the emitted HTML, so a rendered element leads back to
/// the line that wrote it. Tracking costs a stack walk per element; leave it off in production.
///
/// Use [#enabled(Supplier)] for tracked authoring and compilation.
/// Component-generated elements point to the line that placed the component.
public final class SourceTracking {

  private static final StackWalker WALKER =
      StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
  private static final @Nullable String LIBRARY_LOCATION = locationOf(SourceTracking.class);
  private static final ClassValue<Boolean> LIBRARY_CLASSES =
      new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
          return LIBRARY_LOCATION != null && Objects.equals(LIBRARY_LOCATION, locationOf(type));
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

  /// Runs `work` with tracking on; elements it builds or compiles carry their call sites.
  public static <R> R enabled(Supplier<R> work) {
    return with(true, work);
  }

  /// Runs `work` with tracking set to `tracking`. Compilation uses this to carry the caller's
  /// setting onto the threads it compiles on.
  public static <R> R with(boolean tracking, Supplier<R> work) {
    SourceCapture capture = tracking ? SourceTracking::capture : SourceCapture.NONE;
    return SourceProvenance.with(capture, work);
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
        // Reached the compiler before any authoring code: a library component is expanding.
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

  private static @Nullable String locationOf(Class<?> type) {
    CodeSource source = type.getProtectionDomain().getCodeSource();
    var location = source == null ? null : source.getLocation();
    return location == null ? null : location.toString();
  }
}
