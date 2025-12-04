package com.bitwig.extensions.controllers.novation.launch_control_xl.drum;

/**
 * Snapshot of drum UI state for rendering and tests.
 *
 * @param pads The pad states
 * @param selectedPad Index of the selected pad, or -1
 * @param soloMode True if solo mode is active
 * @param muteMode True if mute mode is active
 */
public record DrumUiState(DrumLedRenderer.PadState[] pads, int selectedPad, boolean soloMode, boolean muteMode) {}
