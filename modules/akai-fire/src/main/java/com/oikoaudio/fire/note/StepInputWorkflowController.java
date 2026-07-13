package com.oikoaudio.fire.note;

/** Owns the delayed Bitwig editor workflow and interaction state for live Step Input. */
public final class StepInputWorkflowController {
    private static final long ACTIVATION_DELAY_MS = 100;

    private final Port port;
    private final double displayStepSizeBeats;
    private Phase phase = Phase.INACTIVE;
    private int generation;
    private int stepIndex;
    private int heldPadCount;
    private boolean padGesturePending;
    private double clipLengthBeats;

    public StepInputWorkflowController(final Port port, final double displayStepSizeBeats) {
        this.port = port;
        this.displayStepSizeBeats = displayStepSizeBeats;
    }

    public boolean enabled() {
        return phase == Phase.ACTIVE;
    }

    public Phase phase() {
        return phase;
    }

    public int stepIndex() {
        return stepIndex;
    }

    public void toggle() {
        if (enabled()) {
            deactivate(false);
        } else {
            activate();
        }
    }

    public void activate() {
        if (port.drumMode()) {
            port.display("Note/Harmonic");
            return;
        }
        final int activation = ++generation;
        phase = Phase.OPENING;
        port.showClipInEditor();
        port.display("Opening");
        port.schedule(() -> focusTrack(activation), ACTIVATION_DELAY_MS);
    }

    private void focusTrack(final int activation) {
        if (!current(activation)) {
            return;
        }
        port.selectTrackInEditor();
        port.focusTrackHeader();
        phase = Phase.TRACK_FOCUSED;
        port.schedule(() -> focusEditor(activation), ACTIVATION_DELAY_MS);
    }

    private void focusEditor(final int activation) {
        if (!current(activation)) {
            return;
        }
        if (!port.focusClipEditor()) {
            port.popup("Editor focus unavailable");
            port.logCandidateActions();
        }
        phase = Phase.EDITOR_FOCUSED;
        port.schedule(() -> completeActivation(activation), ACTIVATION_DELAY_MS);
    }

    private void completeActivation(final int activation) {
        if (!current(activation)) {
            return;
        }
        if (!port.activateStepTool()) {
            phase = Phase.INACTIVE;
            port.display("Unavailable");
            port.popup("Tool unavailable");
            port.logCandidateActions();
            return;
        }
        if (!port.moveToFirstItem()) {
            port.popup("Start action unavailable");
            port.logCandidateActions();
        }
        phase = Phase.ACTIVE;
        stepIndex = 0;
        resetGesture();
        port.resetIdleDisplay();
        port.display(stepLabel());
        port.popup("On");
    }

    private boolean current(final int activation) {
        if (activation != generation || port.drumMode()) {
            if (activation == generation) {
                phase = Phase.INACTIVE;
            }
            return false;
        }
        return true;
    }

    public void deactivate(final boolean force) {
        generation++;
        if (!force && !port.activatePointerTool()) {
            port.display("Pointer unavailable");
            port.popup("Pointer tool unavailable");
            port.logCandidateActions();
            return;
        }
        phase = Phase.INACTIVE;
        resetGesture();
        port.resetIdleDisplay();
        port.showLiveIdle();
        port.popup("Off");
    }

    public void clear() {
        generation++;
        if (enabled()) {
            port.activatePointerTool();
        }
        phase = Phase.INACTIVE;
        resetGesture();
    }

    public boolean handleBankButton(final boolean pressed, final int amount, final boolean altHeld) {
        if (!enabled() || !pressed || altHeld) {
            return false;
        }
        if (!port.moveBank(amount)) {
            port.display("Unavailable");
            return true;
        }
        advanceEstimate(amount > 0 ? 1 : -1);
        return true;
    }

    public void handlePadGesture(final boolean pressed) {
        if (!enabled() || port.drumMode()) {
            return;
        }
        if (pressed) {
            if (heldPadCount == 0) {
                padGesturePending = true;
            }
            heldPadCount++;
            return;
        }
        if (heldPadCount > 0) {
            heldPadCount--;
        }
        if (heldPadCount == 0 && padGesturePending) {
            padGesturePending = false;
            advanceEstimate(1);
        }
    }

    public void updateClipLength(final double beats) {
        clipLengthBeats = beats;
        if (enabled()) {
            port.display(stepLabel());
        }
    }

    public String stepLabel() {
        final int displayStep = stepIndex + 1;
        final int totalSteps = estimatedTotalSteps(clipLengthBeats, displayStepSizeBeats);
        return totalSteps > 0 ? "Step %d/%d".formatted(displayStep, totalSteps) : "Step %d".formatted(displayStep);
    }

    static int estimatedTotalSteps(final double clipLengthBeats, final double stepSizeBeats) {
        if (clipLengthBeats <= 0.0 || stepSizeBeats <= 0.0) {
            return -1;
        }
        return Math.max(1, (int) Math.round(clipLengthBeats / stepSizeBeats));
    }

    private void advanceEstimate(final int amount) {
        stepIndex = Math.max(0, stepIndex + amount);
        port.display(stepLabel());
    }

    private void resetGesture() {
        heldPadCount = 0;
        padGesturePending = false;
    }

    public enum Phase {
        INACTIVE,
        OPENING,
        TRACK_FOCUSED,
        EDITOR_FOCUSED,
        ACTIVE
    }

    public interface Port {
        boolean drumMode();
        void showClipInEditor();
        void selectTrackInEditor();
        void schedule(Runnable task, long delayMs);
        boolean focusTrackHeader();
        boolean focusClipEditor();
        boolean activateStepTool();
        boolean activatePointerTool();
        boolean moveToFirstItem();
        boolean moveBank(int amount);
        void display(String value);
        void popup(String value);
        void logCandidateActions();
        void resetIdleDisplay();
        void showLiveIdle();
    }
}
