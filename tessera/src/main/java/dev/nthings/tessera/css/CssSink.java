package dev.nthings.tessera.css;

import dev.nthings.tessera.internal.foundation.Fnv1a64;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/// Receives the rules of a style in emission order: declarations, and nested rules opened with
/// their prelude and closed after their body. [Declarations] and [ScopeStyle] describe their rules
/// once, to any sink: [Text] serializes them and [Hash] folds them into a structural hash, so equal
/// hashes mean equal text by construction.
///
/// A nested rule reaches the sink only once something is written inside it, so a rule that emits
/// nothing leaves no trace in the text or the hash, and [#isEmpty()] tells whether anything was
/// written at all.
abstract sealed class CssSink permits CssSink.Text, CssSink.Hash {

  /// Preludes of the innermost rules opened but not yet written, because nothing has been written
  /// inside them yet.
  private final List<String> pending = new ArrayList<>();

  private boolean written;

  /// Writes `property: value`.
  final void declaration(CssProperty property, String value) {
    flush();
    writeDeclaration(property, value);
  }

  /// Writes the custom property `name: value`.
  final void customProperty(String name, String value) {
    flush();
    writeCustomProperty(name, value);
  }

  /// Opens a nested rule with `prelude`, such as `&:hover` or `@starting-style`.
  final void open(String prelude) {
    pending.add(prelude);
  }

  /// Closes the innermost open rule, which leaves no trace if nothing was written inside it.
  final void close() {
    if (pending.isEmpty()) {
      writeClose();
    } else {
      pending.removeLast();
    }
  }

  /// Receives the keyframes a written style uses, whether or not its rule emits anything.
  void uses(Set<Keyframes> keyframes) {}

  /// Whether nothing has been written.
  final boolean isEmpty() {
    return !written;
  }

  private void flush() {
    written = true;
    for (var prelude : pending) {
      writeOpen(prelude);
    }
    pending.clear();
  }

  abstract void writeDeclaration(CssProperty property, String value);

  abstract void writeCustomProperty(String name, String value);

  abstract void writeOpen(String prelude);

  abstract void writeClose();

  /// Serializes the rules as minified CSS: `a:1;b:2;&:hover{c:3}`.
  static final class Text extends CssSink {
    private final StringBuilder css = new StringBuilder();

    /// Whether a declaration was the last thing written, so the next item needs a separator.
    private boolean afterDeclaration;

    @Override
    void writeDeclaration(CssProperty property, String value) {
      separate().append(property.cssName()).append(':').append(value);
      afterDeclaration = true;
    }

    @Override
    void writeCustomProperty(String name, String value) {
      separate().append(name).append(':').append(value);
      afterDeclaration = true;
    }

    @Override
    void writeOpen(String prelude) {
      separate().append(prelude).append('{');
      afterDeclaration = false;
    }

    @Override
    void writeClose() {
      css.append('}');
      afterDeclaration = false;
    }

    private StringBuilder separate() {
      return afterDeclaration ? css.append(';') : css;
    }

    @Override
    public String toString() {
      return css.toString();
    }
  }

  /// Folds the rules into a 64-bit FNV-1a hash. Each item opens with its kind and each text with
  /// its length, so different rules never fold the same sequence.
  static final class Hash extends CssSink {
    private static final int DECLARATION = 1;
    private static final int CUSTOM_PROPERTY = 2;
    private static final int OPEN = 3;
    private static final int CLOSE = 4;

    private final @Nullable Set<Keyframes> keyframes;
    private long hash = Fnv1a64.OFFSET_BASIS;

    /// A hash that adds the keyframes the hashed styles use to `keyframes`, or ignores them when it
    /// is null.
    Hash(@Nullable Set<Keyframes> keyframes) {
      this.keyframes = keyframes;
    }

    @Override
    void uses(Set<Keyframes> used) {
      if (keyframes != null) {
        keyframes.addAll(used);
      }
    }

    @Override
    void writeDeclaration(CssProperty property, String value) {
      hash = Fnv1a64.updateInt(Fnv1a64.updateByte(hash, DECLARATION), property.ordinal());
      hash = Fnv1a64.updateText(hash, value);
    }

    @Override
    void writeCustomProperty(String name, String value) {
      hash = Fnv1a64.updateText(Fnv1a64.updateByte(hash, CUSTOM_PROPERTY), name);
      hash = Fnv1a64.updateText(hash, value);
    }

    @Override
    void writeOpen(String prelude) {
      hash = Fnv1a64.updateText(Fnv1a64.updateByte(hash, OPEN), prelude);
    }

    @Override
    void writeClose() {
      hash = Fnv1a64.updateByte(hash, CLOSE);
    }

    /// The hash of everything written so far.
    long value() {
      return hash;
    }
  }
}
