package dev.nthings.tessera.wasm.example;

import static dev.nthings.tessera.core.Elements.p;

import dev.nthings.tessera.components.ContentTabs;
import dev.nthings.tessera.components.ContentTabs.Activation;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.InputType;
import dev.nthings.tessera.core.Role;
import dev.nthings.tessera.wasm.BrowserRoot;
import dev.nthings.tessera.wasm.Events;
import dev.nthings.tessera.wasm.Live;
import dev.nthings.tessera.wasm.LiveContent;
import dev.nthings.tessera.wasm.LiveContext;
import dev.nthings.tessera.wasm.LiveElement;
import dev.nthings.tessera.wasm.LiveTabs;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/// Browser-owned mailbox tabs that a background sync and the Delete key change, and app-bound
/// settings tabs that switch only when the application accepts the request.
final class TabsDemo {
  private final List<String> mailboxes =
      new ArrayList<>(List.of("Inbox", "Drafts", "Archive", "Sent"));
  private String current = "Drafts";
  private @Nullable String section = "account";
  private String requested = "none";
  private boolean allowSwitching;

  static BrowserRoot mount(String host) {
    return BrowserRoot.mount(host, new TabsDemo()::view);
  }

  private LiveContent view(LiveContext context) {
    var root = context.root();
    var tabs = ContentTabs.of("Mailboxes").id("mailboxes").initialSelected("Drafts");
    for (var mailbox : mailboxes) {
      if (mailbox.equals("Archive")) {
        tabs.disabledTab(mailbox, mailbox, p().text("The archive is read-only"));
      } else {
        tabs.tab(mailbox, mailbox, p().text(mailbox + " messages"));
      }
    }
    var settings =
        LiveTabs.of(
                ContentTabs.of("Settings")
                    .id("settings")
                    .activation(Activation.MANUAL)
                    .tab("account", "Account", p().text("Account settings"))
                    .tab("billing", "Billing", p().text("Billing settings")))
            .bindSelection(Optional.ofNullable(section))
            .onSelectionRequest(key -> {
              requested = key;
              if (allowSwitching) {
                section = key;
              }
            });
    return LiveElement.of(Elements.section().ariaLabel("Tabs"))
        // A background sync archives the inbox and adds a spam folder a minute after mounting.
        .child(
            Live.effect(
                "sync",
                lifetime ->
                    root.host()
                        .within(lifetime)
                        .timeout(
                            Duration.ofMinutes(1),
                            () -> {
                              mailboxes.remove("Inbox");
                              mailboxes.add("Spam");
                            })))
        .child(
            LiveElement.of(Elements.div())
                .on(
                    Events.KEYDOWN,
                    event -> {
                      if (event.key().equals("Delete")) {
                        root.update(() -> mailboxes.remove(current)).run();
                      }
                    })
                .child(LiveTabs.of(tabs).onSelectionRequest(key -> current = key)))
        .child(settings)
        .children(
            LiveContent.of(Elements.p().role(Role.STATUS).text("Requested: " + requested)),
            LiveElement.of(Elements.label())
                .child(
                    LiveElement.of(Elements.input(InputType.CHECKBOX))
                        .bindChecked(allowSwitching)
                        .onCheckedChange(value -> root.update(() -> allowSwitching = value).run()))
                .text("Allow switching"),
            Ui.button("Restore a removed section", root.update(() -> section = "privacy")),
            Ui.button("Reset settings", root.update(() -> section = "account")));
  }
}
