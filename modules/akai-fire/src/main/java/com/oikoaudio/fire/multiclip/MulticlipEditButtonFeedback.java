package com.oikoaudio.fire.multiclip;

/** OLED descriptions for Multiclip's four sequencer edit buttons. */
final class MulticlipEditButtonFeedback {
    private MulticlipEditButtonFeedback() {}

    static Message message(final int row, final boolean shiftHeld) {
        return switch (row) {
            case 0 -> new Message("Select", "Scene pad");
            case 1 -> new Message("Last Step", "Pattern pad");
            case 2 ->
                    shiftHeld
                            ? new Message("Paste Child Scene", "Scene pad")
                            : new Message("Paste Lane Clip", "Scene pad");
            case 3 -> new Message("Delete", "Pattern pad");
            default -> throw new IllegalArgumentException("Unsupported mute button row: " + row);
        };
    }

    record Message(String title, String detail) {}
}
