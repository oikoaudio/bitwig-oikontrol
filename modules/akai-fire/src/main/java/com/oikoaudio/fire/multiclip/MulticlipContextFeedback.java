package com.oikoaudio.fire.multiclip;

/** Actionable OLED feedback for an incomplete or unselected Multiclip project setup. */
final class MulticlipContextFeedback {
    private MulticlipContextFeedback() {}

    static Message message(
            final MulticlipGroupCursorController.Discovery discovery,
            final boolean groupReady,
            final int childCount,
            final int eligibleChildCount) {
        if (discovery == MulticlipGroupCursorController.Discovery.NOT_FOUND) {
            return new Message("No PolySeq", "Select or name group");
        }
        if (discovery == MulticlipGroupCursorController.Discovery.MULTIPLE) {
            return new Message("Multiple PolySeq", "Select target group");
        }
        if (!groupReady) {
            return new Message("Setup not found", "Select group/child");
        }
        if (childCount == 0) {
            return new Message("No MIDI children", "Add direct tracks");
        }
        if (eligibleChildCount == 0) {
            return new Message("No MIDI children", "Use direct tracks");
        }
        return new Message("Setup unavailable", "Re-enter Multiclip");
    }

    record Message(String title, String detail) {}
}
