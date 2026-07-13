package com.oikoaudio.gradle

import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

class BundledDocumentationRendererTest {
    @Test
    void escapesHtmlBeforeRenderingInlineCodeAndEmphasis() {
        assertEquals(
                '&lt;unsafe attr=&quot;x&quot;&gt; <code>A&amp;B</code> <strong>bold</strong>',
                BundledDocumentationRenderer.renderInlineMarkdown('<unsafe attr="x"> `A&B` **bold**'))
    }

    @Test
    void rendersHeadingsParagraphsAndNestedLists() {
        def html = BundledDocumentationRenderer.renderMarkdownToHtml('''# Guide

First line
continues here.

- Parent
  1. Child one
  2. Child two
''')

        assertTrue(html.contains('      <h1>Guide</h1>\n'))
        assertTrue(html.contains('      <p>First line continues here.</p>\n'))
        assertTrue(html.contains('      <ul>\n        <li>Parent\n      <ol>\n'))
        assertTrue(html.contains('        <li>Child one</li>\n        <li>Child two</li>\n'))
    }

    @Test
    void classifiesControllerTablesAndEncoderRows() {
        def html = BundledDocumentationRenderer.renderMarkdownToHtml('''| Encoder page | Encoder 1 | Encoder 2 | Encoder 3 | Encoder 4 |
| --- | --- | --- | --- | --- |
| Channel | A | B | C | D |
| Mixer | Track volume | Track pan | Send 1 | Send 2 |
| User 1 | Pressure | Timbre | Chance | Other |
| User 2 | Mode A | Mode B | Mode C | Mode D |

| Control | Action |
| --- | --- |
| `SHIFT` | Alternate action |
''')

        assertTrue(html.contains('<table class="encoder-map">'))
        assertTrue(html.contains('<tr class="encoder-primary"><td>Channel</td>'))
        assertTrue(html.contains('<tr class="encoder-shared"><td>Mixer</td>'))
        assertTrue(html.contains('<tr class="encoder-expression"><td>User 1</td>'))
        assertTrue(html.contains('<tr class="encoder-mode"><td>User 2</td>'))
        assertTrue(html.contains('<table class="control-map">'))
    }

    @Test
    void wrapsRenderedMarkdownInBundledDocumentTemplate() {
        def document = BundledDocumentationRenderer.renderUserGuideDocument('## Setup')

        assertTrue(document.startsWith('<!DOCTYPE html>\n<html lang="en">'))
        assertTrue(document.contains('      <h2>Setup</h2>'))
        assertTrue(document.contains('.encoder-map tr.encoder-expression td'))
        assertTrue(document.endsWith('</html>\n'))
    }
}
