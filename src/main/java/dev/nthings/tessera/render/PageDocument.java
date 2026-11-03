package dev.nthings.tessera.render;

import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.css.Tokens;

/** Top-level HTML5 document specification compiled by {@link PageCompiler}. */
public record PageDocument(String lang, String title, Tokens tokens, IntoElement bodyRoot) {

  public PageDocument(String title, IntoElement bodyRoot) {
    this("en", title, new Tokens(), bodyRoot);
  }

  public PageDocument(String lang, String title, IntoElement bodyRoot) {
    this(lang, title, new Tokens(), bodyRoot);
  }
}
