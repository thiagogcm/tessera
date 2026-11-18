package dev.nthings.tessera.wasm;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

/// Runs a browser test in Chromium only, for behavior Java implements the same way in every engine:
/// scheduling, render rejection, effect lifecycles, id ownership and failure reports. The test
/// takes a [Session].
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@TestTemplate
@ExtendWith(BrowserSuite.class)
@interface Chromium {}
