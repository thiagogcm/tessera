# tessera task runner - single source of truth for build, test, site generation, and quality commands.
#
# Quick start:  just            (list recipes)
#               just verify     (unit tests + browser E2E + quality gates)
#               just site       (compile multi-page showcase site into target/site)
#               just demo       (serve multi-page showcase site at http://localhost:8080)
#               just explain /overlays/  (print a route's element tree with Java source lines)

set shell := ["bash", "-euo", "pipefail", "-c"]
set windows-shell := ["bash", "-euo", "pipefail", "-c"]

mvn := "./mvnw -B -ntp"

# List available recipes.
default:
    @just --list

# --- Build --------------------------------------------------------------------

# Compile and package the library jar (skip tests).
[group('build')]
build:
    {{mvn}} -DskipTests clean package

# Compile main and test sources without running tests.
[group('build')]
compile:
    {{mvn}} -DskipTests clean test-compile

# Compile the experimental browser demo with Oracle GraalVM and Binaryen.
[group('build')]
wasm:
    {{mvn}} -Pwasm -pl tessera-wasm-showcase -am -DskipTests package

# Build the actual Wasm image and exercise it in Chromium and Firefox.
[group('test')]
wasm-verify:
    {{mvn}} -Pwasm -pl tessera-wasm-showcase -am verify

# Compile the multi-page showcase static site into target/site.
[group('build')]
site output="target/site":
    {{mvn}} -q compile exec:java -Dexec.mainClass=dev.nthings.tessera.showcase.ShowcaseSite -Dexec.args="--output {{absolute_path(output)}}"

# Build the runnable two-page example from the consumer guide.
[group('build')]
guide output="target/guide":
    {{mvn}} -q compile exec:java -Dexec.mainClass=dev.nthings.tessera.showcase.guide.GuideSite -Dexec.args="{{absolute_path(output)}}"

# Build and serve the multi-page showcase site over HTTP for live browser/DevTools evaluation.
[group('build')]
demo port="8080":
    {{mvn}} -q compile exec:java -Dexec.mainClass=dev.nthings.tessera.showcase.ShowcaseSite -Dexec.args="--output {{justfile_directory()}}/target/site --sources --serve {{port}}"

# Print one showcase route's element tree with the Java line that built each element.
[group('build')]
explain route:
    {{mvn}} -q compile exec:java -Dexec.mainClass=dev.nthings.tessera.showcase.ShowcaseSite -Dexec.args="--explain {{route}}"

# Remove Maven build outputs.
[group('build')]
clean:
    {{mvn}} clean

# --- Test ---------------------------------------------------------------------

# Run unit tests without launching browsers.
[group('test')]
test:
    {{mvn}} test

# Install Playwright Chromium and Firefox; pass --with-deps to install Linux system packages too.
[group('test')]
browsers options="":
    {{mvn}} -q test-compile exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.classpathScope=test -Dexec.args="install {{options}} chromium firefox"

# Canonical gate: unit tests, browser E2E, strict compilation, coverage, and quality checks.
[group('test')]
verify:
    {{mvn}} clean verify

# --- Quality ------------------------------------------------------------------

# Apply Spotless formatting in place.
[group('quality')]
format:
    {{mvn}} spotless:apply

# Verify Spotless formatting without modifying files.
[group('quality')]
format-check:
    {{mvn}} spotless:check

# Enforce zero @SuppressWarnings annotations across the entire source tree.
[group('quality')]
no-suppress-warnings:
    #!/usr/bin/env bash
    set -euo pipefail
    if rg -n '@SuppressWarnings' tessera/src tessera-compiler/src tessera-markdown/src tessera-showcase/src tessera-wasm/src tessera-wasm-showcase/src; then
        echo "ERROR: @SuppressWarnings is forbidden in tessera." >&2
        exit 1
    fi
    echo "OK: Zero @SuppressWarnings found."

# Report the versions of the tools used by the build.
[group('quality')]
doctor:
    #!/usr/bin/env bash
    set -uo pipefail
    echo "java:  $(java -version 2>&1 | head -1)"
    echo "mvnw:  $({{mvn}} -v 2>/dev/null | head -1)"
    command -v just >/dev/null && echo "just:  $(just --version)" || echo "just:  MISSING"

# --- Release ------------------------------------------------------------------

[group('release')]
version:
    @{{mvn}} -q -N help:evaluate -Dexpression=project.version -DforceStdout

# Update the parent and every module, including the opt-in Wasm reactor.
[group('release')]
[positional-arguments]
set-version version:
    {{mvn}} -Pwasm org.codehaus.mojo:versions-maven-plugin:2.25.3:set -DnewVersion="$1" -DprocessAllModules -DgenerateBackupPoms=false

# Verify the JVM reactor and stage the two published modules locally.
[group('release')]
stage:
    {{mvn}} -Ppublication clean deploy

[group('release')]
[positional-arguments]
release version next="" dry="true":
    gh workflow run release.yml --ref main -f "releaseVersion=$1" -f "nextVersion=$2" -f "dryRun=$3"

[group('release')]
release-watch:
    gh run watch "$(gh run list --workflow=release.yml --branch main --limit 1 --json databaseId --jq '.[0].databaseId')" --exit-status
