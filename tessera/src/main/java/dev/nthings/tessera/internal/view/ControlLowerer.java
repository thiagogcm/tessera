package dev.nthings.tessera.internal.view;

import dev.nthings.tessera.core.HtmlTag;
import dev.nthings.tessera.internal.authoring.InitialControl;
import dev.nthings.tessera.view.ControlLowering;
import dev.nthings.tessera.view.DiagnosticReporter;
import dev.nthings.tessera.view.ViewSnapshot.Defaults;
import dev.nthings.tessera.view.ViewSnapshot.Group;
import dev.nthings.tessera.view.ViewSnapshot.Node;
import dev.nthings.tessera.view.ViewSnapshot.Special;
import dev.nthings.tessera.view.ViewSnapshot.Tag;
import dev.nthings.tessera.view.ViewSnapshot.Text;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.owasp.encoder.Encode;

/// Lowers initial state after linking so option discovery sees expanded groups and late content.
final class ControlLowerer {
  private static final Pattern ASCII_WHITESPACE = Pattern.compile("[\\t\\n\\f\\r ]+");

  private ControlLowerer() {}

  /// Uses the same character repertoire as serialized HTML, then undoes text escaping.
  private static String htmlText(String text) {
    return Encode.forHtmlContent(text)
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&");
  }

  static <M> Tag<M> lower(Tag<M> tag, InitialControl initial, DiagnosticReporter reporter) {
    if (initial == InitialControl.NONE) {
      return tag;
    }
    var value = initial.value();
    var checked = initial.checked();
    var selected = initial.selectedValues();
    var attributes = new LinkedHashMap<>(tag.attributes());
    var children = tag.children();
    var inputValue = false;
    var checkedDefault = false;
    var textarea = false;
    var selection = false;
    if (checked != null) {
      if (!ControlLowering.isCheckable(tag.tag(), attributes)) {
        reporter.report(
            tag.location(),
            "initial-checked",
            "Initial checked state requires a checkbox or radio input",
            "Move initialChecked to an input with type checkbox or radio.");
      } else if (!conflict(tag, attributes, "checked", reporter)) {
        checkedDefault = true;
        if (checked) {
          attributes.put("checked", "");
        }
      }
    }
    if (value != null && !conflict(tag, attributes, "value", reporter)) {
      if (tag.tag() == HtmlTag.INPUT && !"file".equals(ControlLowering.inputType(attributes))) {
        inputValue = true;
        attributes.put("value", htmlText(value));
      } else if (tag.tag() == HtmlTag.TEXTAREA) {
        if (hasContent(children)) {
          reporter.report(
              tag.location(),
              "initial-control-conflict",
              "A textarea with initialValue also has authored children",
              "Supply the textarea's default through initialValue and remove its children.");
        } else {
          textarea = true;
          children = List.of(new Text<>(htmlText(value).replace("\r\n", "\n").replace('\r', '\n')));
        }
      } else if (tag.tag() == HtmlTag.SELECT && !attributes.containsKey("multiple")) {
        selection = true;
        children =
            new Options<M>(tag, Set.of(htmlText(value)), false, reporter).lower(children, false);
      } else {
        reporter.report(
            tag.location(),
            "initial-value",
            "An initial value requires an input other than file, a textarea or a single select",
            "Move initialValue to a compatible control or use initialSelectedValues for a multiple select.");
      }
    }
    if (selected != null) {
      if (tag.tag() != HtmlTag.SELECT || !attributes.containsKey("multiple")) {
        reporter.report(
            tag.location(),
            "initial-selection",
            "Initial selected values require a multiple select",
            "Enable multiple selection or use initialValue on a single select.");
      } else if (value == null && !conflict(tag, attributes, "value", reporter)) {
        selection = true;
        var values = new HashSet<String>();
        selected.forEach(valueToSelect -> values.add(htmlText(valueToSelect)));
        children = new Options<M>(tag, values, true, reporter).lower(children, false);
      }
    }
    return tag.withControlMarkup(
        attributes, children, new Defaults(inputValue, checkedDefault, textarea, selection));
  }

  private static boolean conflict(
      Tag<?> tag, Map<String, String> attributes, String name, DiagnosticReporter reporter) {
    if (!attributes.containsKey(name)) {
      return false;
    }
    reporter.report(
        tag.location(),
        "initial-control-conflict",
        "The '" + name + "' attribute conflicts with a typed initial control declaration",
        "Remove the generic attribute and supply the default through the typed declaration.");
    return true;
  }

  private static boolean hasContent(List<? extends Node<?>> children) {
    for (var node : children) {
      if (!(node instanceof Group<?> group) || hasContent(group.children())) {
        return true;
      }
    }
    return false;
  }

  /// Carries the first match across transparent groups in a single select.
  private static final class Options<M> {
    private final Tag<M> select;
    private final Set<String> values;
    private final boolean multiple;
    private final DiagnosticReporter reporter;
    private boolean matched;
    private boolean opaque;

    Options(Tag<M> select, Set<String> values, boolean multiple, DiagnosticReporter reporter) {
      this.select = select;
      this.values = values;
      this.multiple = multiple;
      this.reporter = reporter;
    }

    List<Node<M>> lower(List<Node<M>> children, boolean inOptgroup) {
      var result = new ArrayList<Node<M>>(children.size());
      for (var child : children) {
        result.add(
            switch (child) {
              case Group<M> group ->
                  new Group<>(group.key(), group.definition(), lower(group.children(), inOptgroup));
              case Tag<M> tag -> lowerTag(tag, inOptgroup);
              case Special<M> special -> {
                if (special.readable()) {
                  reportOpaque();
                }
                yield special;
              }
              case Text<M> text -> text;
            });
      }
      return result;
    }

    private Tag<M> lowerTag(Tag<M> tag, boolean inOptgroup) {
      if (tag.tag() == HtmlTag.OPTION) {
        if (conflict(tag, tag.attributes(), "selected", reporter)) {
          return tag;
        }
        var authored = tag.attributes().get("value");
        String value;
        boolean normalizeValue;
        if (authored == null) {
          var text = new StringBuilder();
          appendOptionText(tag.children(), text);
          var raw = text.toString();
          var normalized = htmlText(raw);
          value = stripSpace(ASCII_WHITESPACE.matcher(normalized).replaceAll(" "));
          normalizeValue = !raw.equals(normalized);
        } else {
          value = htmlText(authored);
          normalizeValue = !authored.equals(value);
        }
        var selectOption = (!matched || multiple) && values.contains(value);
        if (!selectOption && !normalizeValue) {
          return tag;
        }
        var attributes = new LinkedHashMap<>(tag.attributes());
        if (normalizeValue) {
          attributes.put("value", value);
        }
        if (selectOption) {
          matched = true;
          attributes.put("selected", "");
        }
        return tag.withControlMarkup(attributes, tag.children(), tag.defaults());
      }
      if (!ControlLowering.mayContainOptions(tag.tag())
          || (tag.tag() == HtmlTag.OPTGROUP && inOptgroup)
          || tag.children().isEmpty()) {
        return tag;
      }
      return tag.withControlMarkup(
          tag.attributes(),
          lower(tag.children(), inOptgroup || tag.tag() == HtmlTag.OPTGROUP),
          tag.defaults());
    }

    /// Removes the single leading and trailing space left by collapsing whitespace runs.
    private static String stripSpace(String collapsed) {
      var start = collapsed.startsWith(" ") ? 1 : 0;
      var end = collapsed.endsWith(" ") ? collapsed.length() - 1 : collapsed.length();
      return start >= end ? "" : collapsed.substring(start, end);
    }

    private void appendOptionText(List<Node<M>> children, StringBuilder text) {
      for (var child : children) {
        switch (child) {
          case Text<M> value -> text.append(value.content());
          case Group<M> group -> appendOptionText(group.children(), text);
          case Tag<M> tag -> appendOptionText(tag.children(), text);
          case Special<M> special -> {
            if (special.readable()) {
              reportOpaque();
            }
          }
        }
      }
    }

    private void reportOpaque() {
      if (!opaque) {
        opaque = true;
        reporter.report(
            select.location(),
            "initial-selection",
            "Options in raw HTML cannot be matched by a typed initial selection",
            "Build options as elements or set the initial selection in the raw HTML.");
      }
    }
  }
}
