package dev.nthings.tessera.compiler;

/// A registered navigation heading with its resolved DOM id, for the search index.
record PageHeading(String domId, String title, int level) {}
