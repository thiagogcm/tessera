module dev.nthings.tessera.markdown {
  requires transitive dev.nthings.tessera.compiler;
  requires static transitive com.google.errorprone.annotations;
  requires static transitive org.jspecify;
  requires org.commonmark;
  requires org.commonmark.ext.autolink;
  requires org.commonmark.ext.footnotes;
  requires org.commonmark.ext.gfm.alerts;
  requires org.commonmark.ext.gfm.strikethrough;
  requires org.commonmark.ext.gfm.tables;
  requires org.commonmark.ext.heading.anchor;
  requires org.commonmark.ext.image.attributes;
  requires org.commonmark.ext.task.list.items;
  requires org.commonmark.ext.front.matter;

  exports dev.nthings.tessera.markdown;
}
