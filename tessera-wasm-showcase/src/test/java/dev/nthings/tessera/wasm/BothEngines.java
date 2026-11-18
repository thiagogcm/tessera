package dev.nthings.tessera.wasm;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

/// Runs a browser test in Chromium and in Firefox, for behavior the engine implements: typing, IME,
/// focus, dialogs, event defaults, media, the security policy and bundle loading. The test takes a
/// [Session] for each engine.
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@TestTemplate
@ExtendWith(BrowserSuite.class)
@interface BothEngines {}
