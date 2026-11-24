package dev.nthings.tessera.internal.foundation;

/// Compiler-facing: deterministic 64-bit FNV-1a hashing, and the identifier sanitization used for
/// DOM ids, transition names and group names. Hashes are the same on every JVM run.
///
/// Sanitization turns characters outside `[A-Za-z0-9_-]` into `-`.
public final class Fnv1a64 {

  /// Compiler-facing: the FNV-1a 64-bit offset basis, the starting value of a hash.
  public static final long OFFSET_BASIS = 0xcbf29ce484222325L;

  private static final long PRIME = 0x100000001b3L;

  private Fnv1a64() {}

  /// Compiler-facing: folds the low byte of `value` into `hash`.
  public static long updateByte(long hash, int value) {
    return (hash ^ (value & 0xFFL)) * PRIME;
  }

  /// Compiler-facing: folds the four bytes of `value`, least significant first, into `hash`.
  public static long updateInt(long hash, int value) {
    var h = hash;
    for (var shift = 0; shift < 32; shift += 8) {
      h = updateByte(h, (value >>> shift) & 0xFF);
    }
    return h;
  }

  /// Compiler-facing: folds each UTF-16 code unit of `chars` into `hash`, low byte first.
  public static long updateChars(long hash, CharSequence chars) {
    var h = hash;
    for (int i = 0, len = chars.length(); i < len; i++) {
      var c = chars.charAt(i);
      h = updateByte(h, c & 0xFF);
      h = updateByte(h, (c >>> 8) & 0xFF);
    }
    return h;
  }

  /// Compiler-facing: folds the length of `text`, then its UTF-16 code units, into `hash`. The
  /// length prefix keeps consecutive texts apart, so `ab`, `c` and `a`, `bc` fold different hashes.
  public static long updateText(long hash, CharSequence text) {
    return updateChars(updateInt(hash, text.length()), text);
  }

  /// Compiler-facing: the unsigned base-36 form of `hash`, used in generated class names.
  public static String toBase36Hash(long hash) {
    return Long.toUnsignedString(hash, 36);
  }

  /// Replaces each character outside `[A-Za-z0-9_-]` with `-`. Throws IllegalArgumentException if
  /// `raw` is empty.
  public static String sanitizeIdentifier(String raw) {
    if (raw.isEmpty()) {
      throw new IllegalArgumentException("Identifier cannot be empty");
    }
    var sb = new StringBuilder(raw.length());
    for (int i = 0, len = raw.length(); i < len; i++) {
      var c = raw.charAt(i);
      if ((c >= 'a' && c <= 'z')
          || (c >= 'A' && c <= 'Z')
          || (c >= '0' && c <= '9')
          || c == '_'
          || c == '-') {
        sb.append(c);
      } else {
        sb.append('-');
      }
    }
    return sb.toString();
  }

  /// Compiler-facing: sanitizes `raw` as [#sanitizeIdentifier(String)] does, then collapses runs of
  /// `-` to one, so the result never contains `--`, the separator of id-scoped DOM ids. Throws
  /// IllegalArgumentException if `raw` is empty.
  public static String sanitizeLocalId(String raw) {
    var sanitized = sanitizeIdentifier(raw);
    var sb = new StringBuilder(sanitized.length());
    for (int i = 0, len = sanitized.length(); i < len; i++) {
      var c = sanitized.charAt(i);
      if (c != '-' || sb.isEmpty() || sb.charAt(sb.length() - 1) != '-') {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  /// Converts a camel-case or Pascal-case name to kebab-case, so `MetricCard` gives `metric-card`.
  /// Other characters become `-`, and an empty name gives `scope`.
  public static String toKebabCase(String camelOrPascal) {
    if (camelOrPascal.isEmpty()) {
      return "scope";
    }
    var sb = new StringBuilder(camelOrPascal.length() + 4);
    for (int i = 0, len = camelOrPascal.length(); i < len; i++) {
      var c = camelOrPascal.charAt(i);
      if (c >= 'A' && c <= 'Z') {
        if (i > 0) {
          sb.append('-');
        }
        sb.append((char) (c + ('a' - 'A')));
      } else if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '-') {
        sb.append(c);
      } else {
        sb.append('-');
      }
    }
    return sb.toString();
  }
}
