package com.oikoaudio.fire.perform;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PerformLayoutTest {

    @Test
    void verticalLayoutMapsPadsByColumnThenRow() {
        final PerformLayout layout = PerformLayout.vertical();

        assertEquals("PerformV", layout.label());
        assertEquals(16, layout.visibleTrackCount());
        assertEquals(4, layout.visibleSceneCount());
        assertEquals(3, layout.visibleTrackIndexForPad(19));
        assertEquals(1, layout.visibleSceneIndexForPad(19));
        assertEquals(19, layout.toPadIndex(3, 1));
    }

    @Test
    void horizontalLayoutMapsPadsByRowThenColumn() {
        final PerformLayout layout = PerformLayout.vertical().toggle();

        assertEquals("PerformH", layout.label());
        assertEquals(4, layout.visibleTrackCount());
        assertEquals(16, layout.visibleSceneCount());
        assertEquals(1, layout.visibleTrackIndexForPad(19));
        assertEquals(3, layout.visibleSceneIndexForPad(19));
        assertEquals(19, layout.toPadIndex(1, 3));
    }
}
