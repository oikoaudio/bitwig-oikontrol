package com.oikoaudio.fire.note;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StepInputWorkflowControllerTest {
    @Test
    void activationRunsTheEditorSequenceAcrossScheduledStages() {
        final FakePort port = new FakePort();
        final StepInputWorkflowController controller = new StepInputWorkflowController(port, 0.25);

        controller.activate();
        assertEquals(List.of("show-clip", "display:Opening"), port.events);
        assertEquals(StepInputWorkflowController.Phase.OPENING, controller.phase());

        port.runAll();

        assertEquals(List.of("show-clip", "display:Opening", "select-track", "focus-track", "focus-editor",
                "step-tool", "first-item", "reset-idle", "display:Step 1", "popup:On"), port.events);
        assertTrue(controller.enabled());
        assertEquals(StepInputWorkflowController.Phase.ACTIVE, controller.phase());
    }

    @Test
    void cancellationMakesEverySupersededTaskInert() {
        final FakePort port = new FakePort();
        final StepInputWorkflowController controller = new StepInputWorkflowController(port, 0.25);

        controller.activate();
        controller.clear();
        port.runAll();

        assertEquals(List.of("show-clip", "display:Opening"), port.events);
        assertFalse(controller.enabled());
        assertEquals(StepInputWorkflowController.Phase.INACTIVE, controller.phase());
    }

    @Test
    void unavailableEditorFocusFallsBackButActivationContinues() {
        final FakePort port = new FakePort();
        port.focusEditor = false;
        final StepInputWorkflowController controller = new StepInputWorkflowController(port, 0.25);

        controller.activate();
        port.runAll();

        assertTrue(controller.enabled());
        assertTrue(port.events.contains("popup:Editor focus unavailable"));
        assertTrue(port.events.contains("log-actions"));
    }

    @Test
    void unavailableStepToolLeavesWorkflowInactive() {
        final FakePort port = new FakePort();
        port.stepTool = false;
        final StepInputWorkflowController controller = new StepInputWorkflowController(port, 0.25);

        controller.activate();
        port.runAll();

        assertFalse(controller.enabled());
        assertEquals(StepInputWorkflowController.Phase.INACTIVE, controller.phase());
        assertTrue(port.events.contains("display:Unavailable"));
        assertTrue(port.events.contains("popup:Tool unavailable"));
    }

    @Test
    void normalDeactivationRequiresPointerButForcedDeactivationDoesNot() {
        final FakePort port = new FakePort();
        final StepInputWorkflowController controller = activeController(port);
        port.pointerTool = false;

        controller.deactivate(false);
        assertTrue(controller.enabled());
        assertTrue(port.events.contains("popup:Pointer tool unavailable"));

        controller.deactivate(true);
        assertFalse(controller.enabled());
        assertFalse(port.events.subList(port.events.size() - 3, port.events.size()).contains("pointer-tool"));
    }

    @Test
    void bankAndCompletedPadGesturesAdvanceTheEstimate() {
        final FakePort port = new FakePort();
        final StepInputWorkflowController controller = activeController(port);
        controller.updateClipLength(4.0);

        assertTrue(controller.handleBankButton(true, 1, false));
        assertEquals(1, controller.stepIndex());
        controller.handlePadGesture(true);
        controller.handlePadGesture(true);
        controller.handlePadGesture(false);
        assertEquals(1, controller.stepIndex());
        controller.handlePadGesture(false);

        assertEquals(2, controller.stepIndex());
        assertEquals("Step 3/16", controller.stepLabel());
        assertTrue(port.events.contains("right"));
    }

    @Test
    void drumModeRejectsActivationAndCancelsAnActivationInFlight() {
        final FakePort port = new FakePort();
        port.drumMode = true;
        final StepInputWorkflowController controller = new StepInputWorkflowController(port, 0.25);

        controller.activate();
        assertEquals(List.of("display:Note/Harmonic"), port.events);

        port.drumMode = false;
        controller.activate();
        port.drumMode = true;
        port.runAll();
        assertFalse(controller.enabled());
    }

    private static StepInputWorkflowController activeController(final FakePort port) {
        final StepInputWorkflowController controller = new StepInputWorkflowController(port, 0.25);
        controller.activate();
        port.runAll();
        port.events.clear();
        return controller;
    }

    private static final class FakePort implements StepInputWorkflowController.Port {
        private final List<String> events = new ArrayList<>();
        private final ArrayDeque<Runnable> tasks = new ArrayDeque<>();
        private boolean drumMode;
        private boolean focusEditor = true;
        private boolean stepTool = true;
        private boolean pointerTool = true;

        void runAll() {
            while (!tasks.isEmpty()) {
                tasks.removeFirst().run();
            }
        }

        @Override public boolean drumMode() { return drumMode; }
        @Override public void showClipInEditor() { events.add("show-clip"); }
        @Override public void selectTrackInEditor() { events.add("select-track"); }
        @Override public void schedule(final Runnable task, final long delayMs) { tasks.add(task); }
        @Override public boolean focusTrackHeader() { events.add("focus-track"); return true; }
        @Override public boolean focusClipEditor() { events.add("focus-editor"); return focusEditor; }
        @Override public boolean activateStepTool() { events.add("step-tool"); return stepTool; }
        @Override public boolean activatePointerTool() { events.add("pointer-tool"); return pointerTool; }
        @Override public boolean moveToFirstItem() { events.add("first-item"); return true; }
        @Override public boolean moveBank(final int amount) { events.add(amount > 0 ? "right" : "left"); return true; }
        @Override public void display(final String value) { events.add("display:" + value); }
        @Override public void popup(final String value) { events.add("popup:" + value); }
        @Override public void logCandidateActions() { events.add("log-actions"); }
        @Override public void resetIdleDisplay() { events.add("reset-idle"); }
        @Override public void showLiveIdle() { events.add("show-live-idle"); }
    }
}
