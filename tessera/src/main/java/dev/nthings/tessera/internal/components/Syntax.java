package dev.nthings.tessera.internal.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/// The syntax highlighting of `components.CodeBlock`: a small regular-expression lexer per language
/// that marks comments, keywords, strings and the like, and leaves the rest as plain text. It reads
/// one source at a time, so block comments and multi-line strings span lines.
public final class Syntax {

  /// What a highlighted span of source is. Each kind has a `--syntax-<kind>` color token.
  public enum Kind {
    COMMENT,
    KEYWORD,
    STRING,
    /// Numbers, booleans, null, and shell variables.
    CONSTANT,
    /// Capitalized names in languages where they name types.
    TYPE,
    /// A name followed by `(`.
    FUNCTION,
    /// Object keys, attributes, and CSS properties.
    PROPERTY,
    /// Markup tags.
    TAG,
    /// Annotations, decorators, doctypes, and directives.
    META;

    /// The class of a span of this kind, as in `tok-keyword`.
    public String cssClass() {
      return "tok-" + name().toLowerCase(Locale.ROOT);
    }
  }

  /// `kind` for the source between `start` and `end`.
  public record Span(int start, int end, Kind kind) {}

  private static final Map<String, Grammar> GRAMMARS = grammars();

  private Syntax() {}

  /// The highlighted spans of `source` in `language`, ordered and disjoint, or empty when the
  /// language is not one of the built-in grammars. The language is matched ignoring case.
  public static Optional<List<Span>> highlight(String language, String source) {
    var grammar = GRAMMARS.get(language.strip().toLowerCase(Locale.ROOT));
    return grammar == null
        ? Optional.empty()
        : Optional.of(grammar.spans(source, 0, source.length()));
  }

  /// One lexing rule: `pattern` at the current position is `kind`, or is lexed again by `inner`.
  private record Rule(Pattern pattern, @Nullable Kind kind, @Nullable Grammar inner) {}

  /// A language: rules tried in order at each position, then a name classified as a keyword,
  /// constant, type or function call.
  private static final class Grammar {
    private final List<Rule> rules = new ArrayList<>();
    private Pattern name = Pattern.compile("[A-Za-z_$][\\w$]*");
    private Set<String> keywords = Set.of();
    private Set<String> constants = Set.of();
    private boolean ignoreCase;
    private boolean types;
    private boolean calls;

    Grammar rule(String regex, Kind kind) {
      rules.add(new Rule(Pattern.compile(regex), kind, null));
      return this;
    }

    Grammar rule(String regex, Grammar inner) {
      rules.add(new Rule(Pattern.compile(regex), null, inner));
      return this;
    }

    Grammar name(String regex) {
      name = Pattern.compile(regex);
      return this;
    }

    Grammar keywords(String words) {
      keywords = Set.of(words.split("\\s+"));
      return this;
    }

    Grammar constants(String words) {
      constants = Set.of(words.split("\\s+"));
      return this;
    }

    /// Matches keywords and constants ignoring case; their lists must be lower case.
    Grammar ignoreCase() {
      ignoreCase = true;
      return this;
    }

    /// Marks capitalized names as types.
    Grammar types() {
      types = true;
      return this;
    }

    /// Marks names followed by `(` as function calls.
    Grammar calls() {
      calls = true;
      return this;
    }

    Grammar copy() {
      var copy = new Grammar();
      copy.rules.addAll(rules);
      copy.name = name;
      copy.keywords = keywords;
      copy.constants = constants;
      copy.ignoreCase = ignoreCase;
      copy.types = types;
      copy.calls = calls;
      return copy;
    }

    List<Span> spans(String source, int from, int to) {
      var spans = new ArrayList<Span>();
      var matchers = rules.stream().map(rule -> rule.pattern().matcher(source)).toList();
      var names = name.matcher(source);
      var position = from;
      while (position < to) {
        var advanced = false;
        for (var i = 0; i < matchers.size(); i++) {
          var matcher = matchers.get(i);
          matcher.region(position, to).useTransparentBounds(true).useAnchoringBounds(false);
          if (matcher.lookingAt() && matcher.end() > position) {
            var rule = rules.get(i);
            var inner = rule.inner();
            if (inner != null) {
              spans.addAll(inner.spans(source, position, matcher.end()));
            } else {
              spans.add(new Span(position, matcher.end(), Objects.requireNonNull(rule.kind())));
            }
            position = matcher.end();
            advanced = true;
            break;
          }
        }
        if (advanced) {
          continue;
        }
        names.region(position, to).useTransparentBounds(true).useAnchoringBounds(false);
        if (names.lookingAt() && names.end() > position) {
          var kind = classify(source, source.substring(position, names.end()), names.end(), to);
          if (kind != null) {
            spans.add(new Span(position, names.end(), kind));
          }
          position = names.end();
        } else {
          position++;
        }
      }
      return spans;
    }

    private @Nullable Kind classify(String source, String word, int end, int to) {
      var key = ignoreCase ? word.toLowerCase(Locale.ROOT) : word;
      if (keywords.contains(key)) {
        return Kind.KEYWORD;
      }
      if (constants.contains(key)) {
        return Kind.CONSTANT;
      }
      if (types && Character.isUpperCase(word.charAt(0))) {
        return Kind.TYPE;
      }
      if (calls) {
        var next = end;
        while (next < to && (source.charAt(next) == ' ' || source.charAt(next) == '\t')) {
          next++;
        }
        if (next < to && source.charAt(next) == '(') {
          return Kind.FUNCTION;
        }
      }
      return null;
    }
  }

  private static final String LINE_COMMENT = "//[^\\n]*";
  private static final String BLOCK_COMMENT = "/\\*[\\s\\S]*?(?:\\*/|\\z)";
  private static final String HASH_COMMENT = "(?<![^\\s;|&(])#[^\\n]*";
  private static final String DOUBLE_QUOTED = "\"(?:[^\"\\\\\\n]|\\\\.)*\"";
  private static final String SINGLE_QUOTED = "'(?:[^'\\\\\\n]|\\\\.)*'";
  private static final String NUMBER =
      "(?<![\\w$.])(?:0[xX][\\da-fA-F_]+|0[bB][01_]+|\\d[\\d_]*(?:\\.\\d[\\d_]*)?"
          + "(?:[eE][+-]?\\d+)?)[a-zA-Z]*";

  private static Map<String, Grammar> grammars() {
    var java =
        new Grammar()
            .rule(LINE_COMMENT, Kind.COMMENT)
            .rule(BLOCK_COMMENT, Kind.COMMENT)
            .rule("\"\"\"[\\s\\S]*?(?:\"\"\"|\\z)", Kind.STRING)
            .rule(DOUBLE_QUOTED, Kind.STRING)
            .rule(SINGLE_QUOTED, Kind.STRING)
            .rule("@(?!interface\\b)[A-Za-z_][\\w.]*", Kind.META)
            .rule(NUMBER, Kind.CONSTANT)
            .keywords(
                "abstract assert boolean break byte case catch char class continue default do"
                    + " double else enum extends final finally float for if implements import"
                    + " instanceof int interface long native new package permits private"
                    + " protected public record return sealed short static strictfp super switch"
                    + " synchronized this throw throws transient try var void volatile while"
                    + " yield")
            .constants("true false null")
            .types()
            .calls();

    var javascript =
        new Grammar()
            .rule(LINE_COMMENT, Kind.COMMENT)
            .rule(BLOCK_COMMENT, Kind.COMMENT)
            .rule("`(?:[^`\\\\]|\\\\[\\s\\S])*(?:`|\\z)", Kind.STRING)
            .rule(DOUBLE_QUOTED, Kind.STRING)
            .rule(SINGLE_QUOTED, Kind.STRING)
            .rule(NUMBER, Kind.CONSTANT)
            .keywords(
                "as async await break case catch class const continue debugger default delete"
                    + " do else export extends finally for from function if import in"
                    + " instanceof let new of return static super switch this throw try typeof"
                    + " var void while with yield")
            .constants("true false null undefined NaN Infinity")
            .types()
            .calls();
    var typescript =
        javascript
            .copy()
            .rule("@[A-Za-z_][\\w.]*", Kind.META)
            .keywords(
                "abstract any as async await bigint boolean break case catch class const"
                    + " continue debugger declare default delete do else enum export extends"
                    + " finally for from function if implements import in infer instanceof"
                    + " interface is keyof let namespace never new number object of private"
                    + " protected public readonly return satisfies static string super switch"
                    + " symbol this throw try type typeof unknown var void while with yield");

    var json =
        new Grammar()
            .rule(LINE_COMMENT, Kind.COMMENT)
            .rule(BLOCK_COMMENT, Kind.COMMENT)
            .rule(DOUBLE_QUOTED + "(?=\\s*:)", Kind.PROPERTY)
            .rule(DOUBLE_QUOTED, Kind.STRING)
            .rule("-?" + NUMBER, Kind.CONSTANT)
            .constants("true false null");

    var css =
        new Grammar()
            .rule(BLOCK_COMMENT, Kind.COMMENT)
            .rule(DOUBLE_QUOTED, Kind.STRING)
            .rule(SINGLE_QUOTED, Kind.STRING)
            .rule("@[\\w-]+", Kind.KEYWORD)
            .rule("!important\\b", Kind.KEYWORD)
            .rule("(?<=[{;]\\s{0,80})-{0,2}[A-Za-z][\\w-]*(?=\\s*:)", Kind.PROPERTY)
            .rule("#[\\da-fA-F]{3,8}\\b", Kind.CONSTANT)
            .rule("(?<![\\w-])-?(?:\\d+\\.?\\d*|\\.\\d+)(?:%|[a-zA-Z]+)?", Kind.CONSTANT)
            .name("-{0,2}[A-Za-z_][\\w-]*")
            .calls();

    var tag =
        new Grammar()
            .rule("\"[^\"]*\"|'[^']*'", Kind.STRING)
            .rule("</?[\\w:.-]+|/?>", Kind.TAG)
            .rule("[^\\s\"'=<>/]+", Kind.PROPERTY);
    var html =
        new Grammar()
            .rule("<!--[\\s\\S]*?(?:-->|\\z)", Kind.COMMENT)
            .rule("<![^>]*>|<\\?[\\s\\S]*?(?:\\?>|\\z)", Kind.META)
            .rule("<\\[CDATA\\[[\\s\\S]*?(?:]]>|\\z)", Kind.STRING)
            .rule("</?[A-Za-z][\\w:.-]*(?:[^<>\"']|\"[^\"]*\"|'[^']*')*>", tag)
            .rule("&[#\\w]+;", Kind.CONSTANT)
            .name("[^<&]+");

    var shell =
        new Grammar()
            .rule(HASH_COMMENT, Kind.COMMENT)
            .rule("'[^']*'", Kind.STRING)
            .rule("\"(?:[^\"\\\\]|\\\\[\\s\\S])*\"", Kind.STRING)
            .rule("\\$(?:\\{[^}\\n]*}|[A-Za-z_]\\w*|[\\d@#?$!*-])", Kind.CONSTANT)
            .name("[A-Za-z_][\\w-]*")
            .keywords(
                "break case continue declare do done elif else esac exit export fi for function"
                    + " if in local readonly return select shift then time trap until unset"
                    + " while");

    var yaml =
        new Grammar()
            .rule(HASH_COMMENT, Kind.COMMENT)
            .rule("(?m)^(?:---|\\.\\.\\.)(?=\\s|$)", Kind.META)
            .rule(
                "(?m)(?<=^[ \\t]{0,80}(?:-[ \\t]{1,8})?)(?:\"[^\"\\n]*\"|'[^'\\n]*'"
                    + "|[^\\s#'\"\\-?:,\\[\\]{}&*!|>][^\\n:#]*?)(?=[ \\t]*:(?:\\s|$))",
                Kind.PROPERTY)
            .rule(DOUBLE_QUOTED, Kind.STRING)
            .rule("'(?:[^']|'')*'", Kind.STRING)
            .rule("[&*][\\w-]+|!!?[\\w-]*", Kind.META)
            .rule("(?<![\\w.-])-?\\d+(?:\\.\\d+)?(?![\\w.-])", Kind.CONSTANT)
            .rule("(?<![\\w-])~(?![\\w-])", Kind.CONSTANT)
            .name("[A-Za-z_][\\w.-]*")
            .constants("true false null yes no on off True False Null TRUE FALSE NULL");

    var python =
        new Grammar()
            .rule("#[^\\n]*", Kind.COMMENT)
            .rule(
                "(?i)[rbuf]{0,2}(?:\"\"\"[\\s\\S]*?(?:\"\"\"|\\z)|'''[\\s\\S]*?(?:'''|\\z))",
                Kind.STRING)
            .rule("(?i)[rbuf]{0,2}(?:" + DOUBLE_QUOTED + "|" + SINGLE_QUOTED + ")", Kind.STRING)
            .rule("(?m)(?<=^[ \\t]{0,80})@[\\w.]+", Kind.META)
            .rule(NUMBER, Kind.CONSTANT)
            .name("[A-Za-z_]\\w*")
            .keywords(
                "and as assert async await break class continue def del elif else except"
                    + " finally for from global if import in is lambda nonlocal not or pass"
                    + " raise return try while with yield")
            .constants("True False None")
            .types()
            .calls();

    var sql =
        new Grammar()
            .rule("--[^\\n]*", Kind.COMMENT)
            .rule(BLOCK_COMMENT, Kind.COMMENT)
            .rule("'(?:[^']|'')*'", Kind.STRING)
            .rule("\"[^\"\\n]*\"|`[^`\\n]*`", Kind.PROPERTY)
            .rule(NUMBER, Kind.CONSTANT)
            .name("[A-Za-z_]\\w*")
            .keywords(
                "add all alter and as asc between by case check column constraint create cross"
                    + " default delete desc distinct drop else end exists foreign from full"
                    + " group having if in index inner insert intersect into is join key left"
                    + " like limit not offset on or order outer primary references returning"
                    + " right select set table then union unique update using values view when"
                    + " where with")
            .constants("null true false")
            .ignoreCase()
            .calls();

    var grammars = new HashMap<String, Grammar>();
    register(grammars, java, "java");
    register(grammars, javascript, "javascript", "js", "mjs", "cjs", "jsx");
    register(grammars, typescript, "typescript", "ts", "mts", "cts", "tsx");
    register(grammars, json, "json", "jsonc", "json5");
    register(grammars, css, "css");
    register(grammars, html, "html", "htm", "xhtml", "xml", "svg");
    register(grammars, shell, "shell", "sh", "bash", "zsh");
    register(grammars, yaml, "yaml", "yml");
    register(grammars, python, "python", "py");
    register(grammars, sql, "sql");
    return Map.copyOf(grammars);
  }

  private static void register(Map<String, Grammar> grammars, Grammar grammar, String... names) {
    for (var name : names) {
      grammars.put(name, grammar);
    }
  }
}
