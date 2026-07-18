package com.oikoaudio.fire.multiclip;

/** Actionable OLED feedback for an incomplete or unselected Multiclip project setup. */
final class MulticlipContextFeedback {
    private MulticlipContextFeedback() {}

    static Message message(
            final boolean groupReady,
            final boolean hasDrumMachine,
            final int childCount,
            final int eligibleChildCount) {
        if (!groupReady) {
            return new Message("Setup not found", "Select group/child");
        }
        if (!hasDrumMachine) {
            return new Message("No Drum Machine", "Add to target group");
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
