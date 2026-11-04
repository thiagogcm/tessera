# tessera task runner - single source of truth for build, test, site generation, and quality commands.
#
# Quick start:  just            (list recipes)
#               just verify     (canonical compile + test + jacoco + spotless gate)
#               just site       (compile multi-page showcase site into target/site)
#               just demo       (serve multi-page showcase site at http://localhost:8080)

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

# Compile the multi-page showcase static site into target/site.
[group('build')]
site output="target/site":
    {{mvn}} -q test-compile exec:java -Dexec.mainClass=dev.nthings.tessera.showcase.ShowcaseSite -Dexec.args="--output {{output}}"

# Build and serve the multi-page showcase site over HTTP for live browser/DevTools evaluation.
[group('build')]
demo port="8080":
    {{mvn}} -q test-compile exec:java -Dexec.mainClass=dev.nthings.tessera.showcase.ShowcaseSite -Dexec.args="--output target/site --serve {{port}}"

# Remove Maven build outputs.
[group('build')]
clean:
    {{mvn}} clean

# --- Test ---------------------------------------------------------------------

# Run unit tests.
[group('test')]
test:
    {{mvn}} test

# Canonical gate: compile (-Xlint:all -Werror + NullAway), test, JaCoCo report, and Spotless check.
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
    if grep -rn "@SuppressWarnings" src/; then
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
