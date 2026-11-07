package dev.nthings.tessera.showcase.e2e;

import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.DisplayName;

@DisplayName("Showcase browser behavior (Chromium)")
class ChromiumShowcaseIT extends ShowcaseBrowserSupport {

  @Override
  protected BrowserType browserType(Playwright playwright) {
    return playwright.chromium();
  }

  @Override
  protected String browserSlug() {
    return "chromium";
  }
}
