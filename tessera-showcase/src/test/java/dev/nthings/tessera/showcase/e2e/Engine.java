package dev.nthings.tessera.showcase.e2e;

import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import java.util.Locale;

/// A browser engine the showcase supports.
enum Engine {
  CHROMIUM,
  FIREFOX;

  BrowserType type(Playwright playwright) {
    return switch (this) {
      case CHROMIUM -> playwright.chromium();
      case FIREFOX -> playwright.firefox();
    };
  }

  @Override
  public String toString() {
    return name().toLowerCase(Locale.ROOT);
  }
}
