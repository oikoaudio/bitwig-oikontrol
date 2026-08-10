package com.oikoaudio.fire.sequence;

/** Pure direct-device navigation for one Drum Machine pad chain. */
final class DrumPadDeviceSelection {
    private DrumPadDeviceSelection() {}

    static int nextIndex(
            final int currentIndex, final int direction, final boolean[] existingDevices) {
        if (direction == 0) {
            return currentIndex;
        }
        int firstExisting = -1;
        for (int index = 0; index < existingDevices.length; index++) {
            if (existingDevices[index]) {
                firstExisting = index;
                break;
            }
        }
        if (firstExisting < 0) {
            return -1;
        }
        final int start = currentIndex < 0 ? firstExisting : currentIndex;
        for (int index = start + Integer.signum(direction);
                index >= 0 && index < existingDevices.length;
                index += Integer.signum(direction)) {
            if (existingDevices[index]) {
                return index;
            }
        }
        return existingDevices[start] ? start : firstExisting;
    }
}
