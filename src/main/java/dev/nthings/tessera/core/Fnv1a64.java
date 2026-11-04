package dev.nthings.tessera.core;

/// Deterministic 64-bit FNV-1a hashing and DOM/CSS identifier sanitization.
///
/// Operates strictly on explicit UTF-16 code units and primitive bytes so hashes are identical
/// across JVM invocations.
public final class Fnv1a64 {

  public static final long OFFSET_BASIS = 0xcbf29ce484222325L;
  private static final long PRIME = 0x100000001b3L;

  private Fnv1a64() {}

  public static long updateByte(long hash, int value) {
    return (hash ^ (value & 0xFFL)) * PRIME;
  }

  public static long updateInt(long hash, int value) {
    long h = hash;
    for (int shift = 0; shift < 32; shift += 8) {
      h = updateByte(h, (value >>> shift) & 0xFF);
    }
    return h;
  }

  public static long updateLong(long hash, long value) {
    long h = hash;
    for (int shift = 0; shift < 64; shift += 8) {
      h = updateByte(h, (int) ((value >>> shift) & 0xFFL));
    }
    return h;
  }

  public static long updateChars(long hash, CharSequence chars) {
    long h = hash;
    for (int i = 0, len = chars.length(); i < len; i++) {
      char c = chars.charAt(i);
      h = updateByte(h, c & 0xFF);
      h = updateByte(h, (c >>> 8) & 0xFF);
    }
    return h;
  }

  public static String toBase36Hash(long hash) {
    return Long.toUnsignedString(hash, 36);
  }

  public static String sanitizeIdentifier(String raw) {
    if (raw.isEmpty()) {
      throw new IllegalArgumentException("Identifier cannot be empty");
    }
    var sb = new StringBuilder(raw.length());
    for (int i = 0, len = raw.length(); i < len; i++) {
      char c = raw.charAt(i);
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

  public static String toKebabCase(String camelOrPascal) {
    if (camelOrPascal.isEmpty()) {
      return "view";
    }
    var sb = new StringBuilder(camelOrPascal.length() + 4);
    for (int i = 0, len = camelOrPascal.length(); i < len; i++) {
      char c = camelOrPascal.charAt(i);
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
