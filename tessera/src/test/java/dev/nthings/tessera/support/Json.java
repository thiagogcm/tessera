package dev.nthings.tessera.support;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/// A strict RFC 8259 reader for generated JSON. Anything a conforming parser such as `JSON.parse`
/// rejects, including raw control characters inside strings, fails the test.
public final class Json {

  /// A parsed value: a [Map] for an object, a [List] for an array, a [String], a [BigDecimal], a
  /// [Boolean] or null.
  public record Node(@Nullable Object value) {

    /// The member `name` of this object; fails if this is not an object or lacks the member.
    public Node get(String name) {
      if (!(value instanceof Map<?, ?> members) || !members.containsKey(name)) {
        throw new AssertionError("Expected a JSON object with \"" + name + "\" but got " + value);
      }
      return new Node(members.get(name));
    }

    /// The member names of this object, in document order.
    public List<String> keys() {
      if (!(value instanceof Map<?, ?> members)) {
        throw new AssertionError("Expected a JSON object but got " + value);
      }
      return members.keySet().stream().map(String::valueOf).toList();
    }

    /// The elements of this array.
    public List<Node> list() {
      if (!(value instanceof List<?> elements)) {
        throw new AssertionError("Expected a JSON array but got " + value);
      }
      return elements.stream().map(Node::new).toList();
    }

    public String string() {
      if (!(value instanceof String string)) {
        throw new AssertionError("Expected a JSON string but got " + value);
      }
      return string;
    }

    public int intValue() {
      if (!(value instanceof BigDecimal number)) {
        throw new AssertionError("Expected a JSON number but got " + value);
      }
      return number.intValueExact();
    }
  }

  private final String text;
  private int at;

  private Json(String text) {
    this.text = text;
  }

  /// The value `text` holds.
  public static Node parse(String text) {
    var reader = new Json(text);
    var value = reader.value();
    reader.whitespace();
    if (reader.at != text.length()) {
      throw reader.error("trailing content");
    }
    return new Node(value);
  }

  private @Nullable Object value() {
    whitespace();
    if (at >= text.length()) {
      throw error("unexpected end");
    }
    return switch (text.charAt(at)) {
      case '{' -> objectValue();
      case '[' -> arrayValue();
      case '"' -> string();
      case 't' -> literal("true", true);
      case 'f' -> literal("false", false);
      case 'n' -> literal("null", null);
      default -> number();
    };
  }

  private Map<String, @Nullable Object> objectValue() {
    var members = new LinkedHashMap<String, @Nullable Object>();
    at++;
    whitespace();
    if (peek('}')) {
      return members;
    }
    do {
      whitespace();
      var name = string();
      whitespace();
      expect(':');
      if (members.containsKey(name)) {
        throw error("duplicate member " + name);
      }
      members.put(name, value());
      whitespace();
    } while (peek(','));
    expect('}');
    return members;
  }

  private List<@Nullable Object> arrayValue() {
    var elements = new ArrayList<@Nullable Object>();
    at++;
    whitespace();
    if (peek(']')) {
      return elements;
    }
    do {
      elements.add(value());
      whitespace();
    } while (peek(','));
    expect(']');
    return elements;
  }

  private String string() {
    expect('"');
    var out = new StringBuilder();
    while (true) {
      if (at >= text.length()) {
        throw error("unterminated string");
      }
      var c = text.charAt(at++);
      if (c == '"') {
        return out.toString();
      }
      if (c < 0x20) {
        throw error("raw control character U+%04X in a string".formatted((int) c));
      }
      if (c != '\\') {
        out.append(c);
        continue;
      }
      var escape = text.charAt(at++);
      switch (escape) {
        case '"', '\\', '/' -> out.append(escape);
        case 'b' -> out.append('\b');
        case 'f' -> out.append('\f');
        case 'n' -> out.append('\n');
        case 'r' -> out.append('\r');
        case 't' -> out.append('\t');
        case 'u' -> {
          out.append((char) Integer.parseInt(text.substring(at, at + 4), 16));
          at += 4;
        }
        default -> throw error("invalid escape \\" + escape);
      }
    }
  }

  private BigDecimal number() {
    var start = at;
    while (at < text.length() && "+-0123456789.eE".indexOf(text.charAt(at)) >= 0) {
      at++;
    }
    var token = text.substring(start, at);
    if (!token.matches("-?(0|[1-9]\\d*)(\\.\\d+)?([eE][+-]?\\d+)?")) {
      throw error("invalid value " + token);
    }
    return new BigDecimal(token);
  }

  private @Nullable Object literal(String word, @Nullable Object value) {
    if (!text.startsWith(word, at)) {
      throw error("invalid literal");
    }
    at += word.length();
    return value;
  }

  private void whitespace() {
    while (at < text.length() && " \t\n\r".indexOf(text.charAt(at)) >= 0) {
      at++;
    }
  }

  private boolean peek(char c) {
    if (at < text.length() && text.charAt(at) == c) {
      at++;
      return true;
    }
    return false;
  }

  private void expect(char c) {
    if (!peek(c)) {
      throw error("expected '" + c + "'");
    }
  }

  private AssertionError error(String problem) {
    return new AssertionError("Invalid JSON at offset " + at + ": " + problem + " in " + text);
  }
}
