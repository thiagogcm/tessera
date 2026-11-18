package dev.nthings.tessera.view;

import dev.nthings.tessera.core.HtmlTag;
import dev.nthings.tessera.core.Role;
import dev.nthings.tessera.view.ViewSnapshot.Tag;
import dev.nthings.tessera.view.ViewSnapshot.Text;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import org.jspecify.annotations.Nullable;

/// Accessibility contracts on the linked tree: role requirements, landmark and heading structure,
/// focus order and label targets.
///
/// Role rules apply to elements that declare a role; native elements are checked where their
/// semantics are unambiguous (`<dialog>`, headings, landmarks, `<label for>`).
final class AccessibilityRules {

  private static final Set<Role> NAMED_LANDMARKS =
      Set.of(Role.NAVIGATION, Role.COMPLEMENTARY, Role.SEARCH, Role.FORM, Role.REGION);

  private final ViewValidation state;
  private final Map<Role, List<Landmark>> landmarks = new EnumMap<>(Role.class);
  private final List<Heading> headings = new ArrayList<>();
  private int mainLandmarks;

  private AccessibilityRules(ViewValidation state) {
    this.state = state;
  }

  private record Landmark(Tag<?> tag, @Nullable String name) {}

  private record Heading(Tag<?> tag, int level) {}

  /// Checks the body of a document or mounted root; `<head>` content has no accessibility tree.
  static void check(List<? extends ViewSnapshot.Node<?>> body, ViewValidation state) {
    var rules = new AccessibilityRules(state);
    visit(body, Context.ROOT, rules::checkTag);
  }

  /// Checks the landmark and heading structure of a whole document's body.
  static void checkDocument(List<? extends ViewSnapshot.Node<?>> body, ViewValidation state) {
    var rules = new AccessibilityRules(state);
    visit(
        body,
        Context.ROOT,
        (tag, context) -> {
          var role = context.unexposed() ? null : effectiveRole(tag);
          if (role != null) {
            rules.recordStructure(tag, role);
          }
        });
    rules.checkLandmarks();
    rules.checkHeadings();
  }

  /// What the surrounding elements imply for a node: `labelled` inside a `<label>`; `unexposed`
  /// inside a closed dialog, popover, or `aria-hidden` subtree, which has no accessibility tree.
  private record Context(boolean labelled, boolean unexposed) {
    static final Context ROOT = new Context(false, false);
  }

  /// Runs `check` on each element of `nodes`, in document order, with its context.
  private static void visit(
      List<? extends ViewSnapshot.Node<?>> nodes,
      Context context,
      BiConsumer<Tag<?>, Context> check) {
    for (var node : nodes) {
      switch (node) {
        case Text<?> _, ViewSnapshot.Special<?> _ -> {}
        case ViewSnapshot.Group<?> group -> visit(group.children(), context, check);
        case Tag<?> tag -> {
          check.accept(tag, context);
          var inner =
              new Context(
                  context.labelled() || tag.tag() == HtmlTag.LABEL,
                  context.unexposed() || isUnexposed(tag));
          visit(tag.children(), inner, check);
        }
      }
    }
  }

  private static boolean isUnexposed(Tag<?> tag) {
    var attrs = tag.attributes();
    return tag.tag() == HtmlTag.DIALOG
        || attrs.containsKey("popover")
        || "true".equals(attrs.get("aria-hidden"));
  }

  private void checkTag(Tag<?> tag, Context context) {
    var attrs = tag.attributes();
    checkTabIndex(tag);
    if (tag.tag() == HtmlTag.LABEL) {
      checkLabelTarget(tag);
    }
    if ("true".equals(attrs.get("aria-hidden"))) {
      checkHiddenFocus(tag);
    }

    var explicit = explicitRole(tag);
    if (explicit != null) {
      checkDeclaredRole(tag, explicit, context);
    } else if (tag.tag() == HtmlTag.DIALOG
        && !context.unexposed()
        && !hasName(tag, null, context)) {
      reportMissingName(tag, Role.DIALOG);
    }

    if (!context.unexposed() && tag.tag().headingLevel() > 0) {
      checkHeadingText(tag);
    }
  }

  // --- Declared roles ---

  private void checkDeclaredRole(Tag<?> tag, Role role, Context context) {
    var attrs = tag.attributes();

    var supported = role.supportedAria();
    for (var name : attrs.keySet()) {
      if (name.startsWith("aria-") && !supported.contains(name)) {
        state.report(
            tag.location(),
            "aria-attribute-not-allowed",
            describe(tag, role) + " does not support " + name,
            "Remove "
                + name
                + ", or use a role that supports it (see Role#supportedAria for the role).");
      }
    }

    var implicit = implicitRole(tag);
    for (var required : role.requiredAria()) {
      // A native element whose own role requires the state already exposes it.
      var exposedNatively = implicit != null && implicit.requiredAria().contains(required);
      if (!attrs.containsKey(required) && !exposedNatively) {
        state.report(
            tag.location(),
            "role-required-attribute",
            describe(tag, role) + " must declare " + required,
            "Declare "
                + required
                + " with its typed aria method, or use a native element that exposes it.");
      }
    }

    var children = role.requiredChildren();
    if (!children.isEmpty() && !ownsAny(tag, children)) {
      state.report(
          tag.location(),
          "role-required-children",
          describe(tag, role)
              + " must contain at least one element with role "
              + children.stream().map(Role::wireValue).sorted().toList(),
          "Add the required child elements, or choose a role that does not require them.");
    }

    if (role.naming() == Role.Naming.REQUIRED
        && !context.unexposed()
        && !hasName(tag, role, context)) {
      reportMissingName(tag, role);
    }
  }

  private static String describe(Tag<?> tag, Role role) {
    return "<" + tag.tag().tagName() + "> with role=\"" + role.wireValue() + "\"";
  }

  private void reportMissingName(Tag<?> tag, Role role) {
    state.report(
        tag.location(),
        "role-name-required",
        describe(tag, role) + " has no accessible name",
        "Add .ariaLabel(...) or .ariaLabelledBy(...)"
            + (role.namedFromContent() ? ", or readable content" : "")
            + ".");
  }

  /// Whether `tag` has an accessible name from an author label, native label, or content.
  private boolean hasName(Tag<?> tag, @Nullable Role role, Context context) {
    if (ViewValidation.hasAuthorName(tag)) {
      return true;
    }
    var attrs = tag.attributes();
    return switch (tag.tag()) {
      case HtmlTag.IMG -> ViewValidation.isNonBlank(attrs.get("alt"));
      case HtmlTag.INPUT, HtmlTag.SELECT, HtmlTag.TEXTAREA -> {
        var id = attrs.get("id");
        yield context.labelled()
            || (id != null && state.labelledControlIds.contains(id))
            || (tag.tag() == HtmlTag.INPUT && isButtonInput(attrs) && attrs.containsKey("value"));
      }
      default ->
          role != null
              && role.namedFromContent()
              && ViewValidation.containsReadableText(tag.children());
    };
  }

  private static boolean isButtonInput(Map<String, String> attrs) {
    var type = attrs.getOrDefault("type", "text");
    return type.equals("button") || type.equals("submit") || type.equals("reset");
  }

  private static boolean ownsAny(Tag<?> tag, Set<Role> roles) {
    return ownsAny(tag.children(), roles);
  }

  private static boolean ownsAny(List<? extends ViewSnapshot.Node<?>> children, Set<Role> roles) {
    for (var child : children) {
      if (child instanceof ViewSnapshot.Group<?> group && ownsAny(group.children(), roles)) {
        return true;
      }
      if (child instanceof Tag<?> nested && hasOrOwnsAny(nested, roles)) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasOrOwnsAny(Tag<?> tag, Set<Role> roles) {
    var role = effectiveRole(tag);
    return (role != null && roles.contains(role)) || ownsAny(tag, roles);
  }

  // --- Roles, explicit and implicit ---

  private static @Nullable Role explicitRole(Tag<?> tag) {
    var declared = tag.attributes().get("role");
    return declared == null ? null : Role.fromWireValue(declared);
  }

  private static @Nullable Role effectiveRole(Tag<?> tag) {
    var explicit = explicitRole(tag);
    return explicit != null ? explicit : implicitRole(tag);
  }

  /// Implicit role of a native element, for elements whose semantics are unconditional.
  private static @Nullable Role implicitRole(Tag<?> tag) {
    var attrs = tag.attributes();
    return switch (tag.tag()) {
      case HtmlTag.MAIN -> Role.MAIN;
      case HtmlTag.NAV -> Role.NAVIGATION;
      case HtmlTag.ASIDE -> Role.COMPLEMENTARY;
      case HtmlTag.SEARCH -> Role.SEARCH;
      case HtmlTag.DIALOG -> Role.DIALOG;
      case HtmlTag.UL, HtmlTag.OL -> Role.LIST;
      case HtmlTag.LI -> Role.LISTITEM;
      case HtmlTag.H1, HtmlTag.H2, HtmlTag.H3, HtmlTag.H4, HtmlTag.H5, HtmlTag.H6 -> Role.HEADING;
      case HtmlTag.BUTTON -> Role.BUTTON;
      case HtmlTag.A -> attrs.containsKey("href") ? Role.LINK : null;
      case HtmlTag.IMG -> "".equals(attrs.get("alt")) ? Role.PRESENTATION : Role.IMG;
      case HtmlTag.TABLE -> Role.TABLE;
      case HtmlTag.HR -> Role.SEPARATOR;
      case HtmlTag.ARTICLE -> Role.ARTICLE;
      case HtmlTag.OUTPUT -> Role.STATUS;
      case HtmlTag.OPTION -> Role.OPTION;
      case HtmlTag.TEXTAREA -> Role.TEXTBOX;
      case HtmlTag.SELECT -> Role.COMBOBOX;
      case HtmlTag.FORM -> ViewValidation.hasAriaName(tag) ? Role.FORM : null;
      case HtmlTag.SECTION -> ViewValidation.hasAriaName(tag) ? Role.REGION : null;
      case HtmlTag.INPUT ->
          switch (attrs.getOrDefault("type", "text")) {
            case "hidden" -> null;
            case "checkbox" -> Role.CHECKBOX;
            case "radio" -> Role.RADIO;
            case "range" -> Role.SLIDER;
            case "number" -> Role.SPINBUTTON;
            case "search" -> Role.SEARCHBOX;
            case "button", "submit", "reset", "image" -> Role.BUTTON;
            default -> Role.TEXTBOX;
          };
      default -> null;
    };
  }

  // --- Document structure ---

  private void recordStructure(Tag<?> tag, Role role) {
    if (role == Role.MAIN) {
      if (++mainLandmarks == 2) {
        state.report(
            tag.location(),
            "multiple-main",
            "A page has one main landmark; this is a second <" + tag.tag().tagName() + ">",
            "Keep a single <main> (or role=\"main\") per page.");
      }
    } else if (NAMED_LANDMARKS.contains(role)) {
      landmarks.computeIfAbsent(role, _ -> new ArrayList<>()).add(new Landmark(tag, nameKey(tag)));
    } else if (role == Role.HEADING) {
      var level = tag.tag().headingLevel();
      if (level == 0) {
        level = ariaLevel(tag);
      }
      if (level > 0) {
        headings.add(new Heading(tag, level));
      }
    }
  }

  private static int ariaLevel(Tag<?> tag) {
    var declared = tag.attributes().get("aria-level");
    // Only TagElement.ariaLevel writes it, already checked to be 1-6.
    return declared == null ? 0 : Integer.parseInt(declared);
  }

  private static int parseIntOr(String text, int fallback) {
    try {
      return Integer.parseInt(text.strip());
    } catch (NumberFormatException invalid) {
      return fallback;
    }
  }

  /// Comparable form of the author-supplied name: label text, or the text of the elements that
  /// label it; `null` when there is none.
  private @Nullable String nameKey(Tag<?> tag) {
    var label = tag.attributes().get("aria-label");
    if (label != null && !label.isBlank()) {
      return label.strip();
    }
    var labelledBy = tag.attributes().get("aria-labelledby");
    if (labelledBy == null || labelledBy.isBlank()) {
      return null;
    }
    var text = new StringBuilder();
    for (var id : labelledBy.strip().split("\\s+")) {
      var target = state.elementsById.get(id);
      if (target != null) {
        text.append(textOf(target)).append(' ');
      }
    }
    var joined = text.toString().strip();
    return joined.isEmpty() ? labelledBy.strip() : joined;
  }

  private static String textOf(ViewSnapshot.Node<?> node) {
    return switch (node) {
      case Text(var content) -> content;
      case Tag<?> tag -> textOf(tag.children());
      case ViewSnapshot.Group<?> group -> textOf(group.children());
      default -> "";
    };
  }

  private static String textOf(List<? extends ViewSnapshot.Node<?>> children) {
    var text = new StringBuilder();
    children.forEach(child -> text.append(textOf(child)));
    return text.toString();
  }

  private void checkLandmarks() {
    for (var entry : landmarks.entrySet()) {
      var group = entry.getValue();
      if (group.size() < 2) {
        continue;
      }
      var seen = new HashSet<String>();
      for (var landmark : group) {
        var name = landmark.name();
        if (name == null) {
          state.report(
              landmark.tag().location(),
              "landmark-name-unique",
              "This page has "
                  + group.size()
                  + " "
                  + entry.getKey().wireValue()
                  + " landmarks, and this one has no accessible name to tell them apart",
              "Give each one a distinct .ariaLabel(...) or .ariaLabelledBy(...).");
        } else if (!seen.add(name)) {
          state.report(
              landmark.tag().location(),
              "landmark-name-unique",
              "Two "
                  + entry.getKey().wireValue()
                  + " landmarks share the accessible name \""
                  + name
                  + "\"",
              "Give each one a distinct accessible name.");
        }
      }
    }
  }

  private void checkHeadings() {
    Heading previous = null;
    for (var heading : headings) {
      if (previous != null && heading.level() > previous.level() + 1) {
        state.report(
            heading.tag().location(),
            "heading-level-skip",
            "Heading level jumps from "
                + previous.level()
                + " to "
                + heading.level()
                + " (<"
                + heading.tag().tag().tagName()
                + ">)",
            "Use level "
                + (previous.level() + 1)
                + " here, or add the intermediate heading so the outline has no gaps.");
      }
      previous = heading;
    }
  }

  private void checkHeadingText(Tag<?> tag) {
    // A declared heading role is named under role-name-required instead.
    if (!ViewValidation.declaresRole(tag) && !hasHeadingText(tag)) {
      state.report(
          tag.location(),
          "empty-heading",
          "<" + tag.tag().tagName() + "> heading has no readable text",
          "Give the heading text or an aria-label, or use an element that is not a heading.");
    }
  }

  private static boolean hasHeadingText(Tag<?> tag) {
    return ViewValidation.hasAriaName(tag) || ViewValidation.containsReadableText(tag.children());
  }

  // --- Focus and labels ---

  private void checkTabIndex(Tag<?> tag) {
    var declared = tag.attributes().get("tabindex");
    if (declared != null && tabIndex(declared) > 0) {
      state.report(
          tag.location(),
          "tabindex-positive",
          "tabindex=\""
              + declared
              + "\" on <"
              + tag.tag().tagName()
              + "> overrides the natural tab order",
          "Use tabindex=\"0\" to make an element focusable, or \"-1\" for scripted focus, and order"
              + " the document instead.");
    }
  }

  private static int tabIndex(String declared) {
    return parseIntOr(declared, Integer.MIN_VALUE);
  }

  private void checkLabelTarget(Tag<?> label) {
    var targetId = label.attributes().get("for");
    var target = targetId == null ? null : state.elementsById.get(targetId);
    if (target != null && !isLabelable(target)) {
      state.report(
          label.location(),
          "label-target",
          "<label for=\""
              + targetId
              + "\"> points at <"
              + target.tag().tagName()
              + ">, which a label cannot name",
          "Point the label at an <input>, <select>, <textarea>, <button>, or <output>, or use"
              + " aria-labelledby on the element instead.");
    }
  }

  private static boolean isLabelable(Tag<?> tag) {
    return switch (tag.tag()) {
      case HtmlTag.BUTTON, HtmlTag.SELECT, HtmlTag.TEXTAREA, HtmlTag.OUTPUT -> true;
      case HtmlTag.INPUT -> !"hidden".equals(tag.attributes().get("type"));
      default -> false;
    };
  }

  private void checkHiddenFocus(Tag<?> hidden) {
    var focusable = firstFocusable(hidden);
    if (focusable != null) {
      state.report(
          hidden.location(),
          "aria-hidden-focusable",
          "<"
              + hidden.tag().tagName()
              + " aria-hidden=\"true\"> contains a focusable <"
              + focusable.tag().tagName()
              + ">, which keyboard users can still reach",
          "Remove aria-hidden, take the control out of the tab order with tabindex=\"-1\", or"
              + " disable it.");
    }
  }

  private static @Nullable Tag<?> firstFocusable(Tag<?> tag) {
    if (isFocusable(tag)) {
      return tag;
    }
    return firstFocusable(tag.children());
  }

  private static @Nullable Tag<?> firstFocusable(List<? extends ViewSnapshot.Node<?>> children) {
    for (var child : children) {
      if (child instanceof ViewSnapshot.Group<?> group) {
        var found = firstFocusable(group.children());
        if (found != null) {
          return found;
        }
      } else if (child instanceof Tag<?> nested && nested.tag() != HtmlTag.DIALOG) {
        var found = firstFocusable(nested);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }

  private static boolean isFocusable(Tag<?> tag) {
    var attrs = tag.attributes();
    if (attrs.containsKey("disabled")) {
      return false;
    }
    var declared = attrs.get("tabindex");
    if (declared != null) {
      return tabIndex(declared) >= 0;
    }
    return switch (tag.tag()) {
      case HtmlTag.A -> attrs.containsKey("href");
      case HtmlTag.BUTTON, HtmlTag.SELECT, HtmlTag.TEXTAREA, HtmlTag.SUMMARY -> true;
      case HtmlTag.INPUT -> !"hidden".equals(attrs.get("type"));
      default -> false;
    };
  }
}
