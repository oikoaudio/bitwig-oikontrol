package com.oikoaudio.gradle

import java.nio.charset.StandardCharsets

final class BundledDocumentationRenderer {
    private static final String TEMPLATE_RESOURCE = '/bundled-documentation-template.html'

    private BundledDocumentationRenderer() {}

    static String escapeHtml(String text) {
        text
                .replace('&', '&amp;')
                .replace('<', '&lt;')
                .replace('>', '&gt;')
                .replace('"', '&quot;')
    }

    static String renderInlineMarkdown(String text) {
        def escaped = escapeHtml(text)
        escaped = escaped.replaceAll(/`([^`]+)`/, '<code>$1</code>')
        escaped.replaceAll(/\*\*([^*]+)\*\*/, '<strong>$1</strong>')
    }

    static List<String> splitMarkdownTableRow(String line) {
        def trimmed = line.trim()
        if (trimmed.startsWith('|')) {
            trimmed = trimmed.substring(1)
        }
        if (trimmed.endsWith('|')) {
            trimmed = trimmed.substring(0, trimmed.length() - 1)
        }
        trimmed.split(/\|/, -1).collect { it.trim() }
    }

    static String tableClassForHeaders(List<String> headers) {
        if (headers == ['Encoder page', 'Encoder 1', 'Encoder 2', 'Encoder 3', 'Encoder 4']
                || headers == ['Encoder page', 'Encoders']) {
            return ' class="encoder-map"'
        }
        if (headers == ['Control', 'Action'] || headers == ['Left-side button', 'Action']) {
            return ' class="control-map"'
        }
        ''
    }

    static String tableRowClassForCells(List<String> headers, List<String> cells) {
        if (cells.isEmpty()) {
            return ''
        }
        def rowLabel = cells[0].replace('`', '').trim()
        def dataCells = cells.drop(1).collect { it.replace('`', '').trim().toLowerCase(Locale.ROOT) }
        def rowText = cells.join(' ').toLowerCase(Locale.ROOT)
        if (headers[0] == 'Encoder page') {
            def remotePage = rowText.contains('remote')
            def mixerPage = rowLabel == 'Mixer' && (
                    rowText.contains('track volume') || rowText.contains('track pan')
                            || rowText.contains('selected pad volume') || rowText.contains('selected pad pan')
                            || rowText.contains('selected track volume') || rowText.contains('selected track pan')
                            || dataCells == ['volume', 'pan', 'send 1', 'send 2']
                            || dataCells == ['track volume', 'track pan', 'send 1', 'send 2']
            )
            if (remotePage || mixerPage) {
                return ' class="encoder-shared"'
            }
            if (rowLabel == 'Channel') {
                return ' class="encoder-primary"'
            }
            if (rowText.contains('expression') || rowText.contains('pressure') || rowText.contains('timbre')
                    || rowText.contains('aftertouch') || rowText.contains('velocity spread')
                    || rowText.contains('held-hit') || rowText.contains('chance')) {
                return ' class="encoder-expression"'
            }
            return ' class="encoder-mode"'
        }
        ''
    }

    static String renderMarkdownToHtml(String markdown) {
        def html = new StringBuilder()
        def lines = markdown.readLines()
        def paragraph = []
        def listStack = []
        def inTable = false
        def tableHeaders = []

        def closeParagraph = {
            if (!paragraph.isEmpty()) {
                html << '      <p>' << renderInlineMarkdown(paragraph.join(' ')) << '</p>\n'
                paragraph.clear()
            }
        }

        def closeOneList = {
            def list = listStack.remove(listStack.size() - 1)
            if (list.liOpen) {
                html << '</li>\n'
            }
            html << "      </${list.type}>\n"
        }

        def closeAllLists = {
            while (!listStack.isEmpty()) {
                closeOneList()
            }
        }

        def closeListsDeeperThan
        closeListsDeeperThan = { int indent ->
            while (!listStack.isEmpty() && listStack.last().indent > indent) {
                closeOneList()
            }
        }

        def ensureList = { String type, int indent ->
            closeListsDeeperThan(indent)
            if (!listStack.isEmpty() && listStack.last().indent == indent && listStack.last().type != type) {
                closeOneList()
            }
            if (listStack.isEmpty() || listStack.last().indent < indent) {
                if (!listStack.isEmpty() && listStack.last().liOpen) {
                    html << '\n'
                }
                html << "      <${type}>\n"
                listStack << [type: type, indent: indent, liOpen: false]
            }
        }

        def closeTable = {
            if (inTable) {
                html << '      </tbody>\n'
                html << '      </table>\n'
                inTable = false
            }
        }

        for (int i = 0; i < lines.size(); i++) {
            def line = lines[i]
            def trimmed = line.trim()

            if (trimmed.isEmpty()) {
                closeParagraph()
                closeAllLists()
                closeTable()
                continue
            }

            if (trimmed.startsWith('|') && i + 1 < lines.size()
                    && lines[i + 1].trim() ==~ /^\|?[\s:-]+\|[\s|:-]*$/) {
                closeParagraph()
                closeAllLists()
                closeTable()
                def headers = splitMarkdownTableRow(trimmed)
                tableHeaders = headers
                html << '      <table' << tableClassForHeaders(headers) << '>\n'
                html << '        <thead><tr>'
                headers.each { html << '<th>' << renderInlineMarkdown(it) << '</th>' }
                html << '</tr></thead>\n'
                html << '        <tbody>\n'
                inTable = true
                i++
                continue
            }

            if (inTable && trimmed.startsWith('|')) {
                def cells = splitMarkdownTableRow(trimmed)
                html << '          <tr' << tableRowClassForCells(tableHeaders, cells) << '>'
                cells.each { html << '<td>' << renderInlineMarkdown(it) << '</td>' }
                html << '</tr>\n'
                continue
            }
            closeTable()

            def heading = trimmed =~ /^(#{1,4})\s+(.+)$/
            if (heading.matches()) {
                closeParagraph()
                closeAllLists()
                def level = heading[0][1].length()
                html << "      <h${level}>" << renderInlineMarkdown(heading[0][2]) << "</h${level}>\n"
                continue
            }

            def unordered = line =~ /^\s*[-*]\s+(.+)$/
            def ordered = line =~ /^\s*\d+\.\s+(.+)$/
            if (unordered.matches() || ordered.matches()) {
                closeParagraph()
                closeTable()
                def wantedType = unordered.matches() ? 'ul' : 'ol'
                def indent = line.takeWhile { it == ' ' }.length()
                ensureList(wantedType, indent)
                if (listStack.last().liOpen) {
                    html << '</li>\n'
                }
                def item = unordered.matches() ? unordered[0][1] : ordered[0][1]
                html << '        <li>' << renderInlineMarkdown(item)
                listStack.last().liOpen = true
                continue
            }

            closeAllLists()
            paragraph << trimmed
        }

        closeParagraph()
        closeAllLists()
        closeTable()
        html.toString()
    }

    static String renderUserGuideDocument(String markdown) {
        def stream = BundledDocumentationRenderer.getResourceAsStream(TEMPLATE_RESOURCE)
        if (stream == null) {
            throw new IllegalStateException("Missing bundled documentation template: ${TEMPLATE_RESOURCE}")
        }
        stream.withCloseable {
            new String(it.readAllBytes(), StandardCharsets.UTF_8)
                    .replace('@@CONTENT@@', renderMarkdownToHtml(markdown))
        }
    }
}
