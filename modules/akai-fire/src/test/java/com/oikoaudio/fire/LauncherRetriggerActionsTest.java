package com.oikoaudio.fire;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bitwig.extension.controller.api.Action;
import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.PinnableCursorClip;
import org.junit.jupiter.api.Test;

class LauncherRetriggerActionsTest {
    @Test
    void resolvesLikelyGlobalRetriggerActionId() {
        final Application application = mock(Application.class);
        final Action action = mock(Action.class);
        when(application.getAction("retrigger_playing_launcher_clips")).thenReturn(action);

        assertSame(
                action,
                LauncherRetriggerActions.resolveRetriggerPlayingLauncherClipsAction(application));
    }

    @Test
    void fallsBackToGuiLabelForGlobalRetriggerAction() {
        final Application application = mock(Application.class);
        final Action other = action("other_action", "Other", "Other");
        final Action target = action("internal_unknown", "Retrigger playing Launcher clips", "");
        when(application.getActions()).thenReturn(new Action[] {other, target});

        assertSame(
                target,
                LauncherRetriggerActions.resolveRetriggerPlayingLauncherClipsAction(application));
    }

    @Test
    void invokesGlobalRetriggerActionWhenAvailable() {
        final Application application = mock(Application.class);
        final Action action = mock(Action.class);
        when(application.getAction("retrigger_playing_launcher_clips")).thenReturn(action);

        assertTrue(LauncherRetriggerActions.retriggerPlayingLauncherClips(application));

        verify(action).invoke();
    }

    @Test
    void reportsUnavailableWhenGlobalRetriggerActionIsMissing() {
        final Application application = mock(Application.class);
        when(application.getActions()).thenReturn(new Action[0]);

        assertFalse(LauncherRetriggerActions.retriggerPlayingLauncherClips(application));
    }

    @Test
    void launchesSelectedClipForCurrentClipRetrigger() {
        final PinnableCursorClip selectedClip = mock(PinnableCursorClip.class);

        LauncherRetriggerActions.retriggerCurrentClip(selectedClip);

        verify(selectedClip).launchWithOptions("default", "from_start");
    }

    @Test
    void ignoresMissingSelectedClipForCurrentClipRetrigger() {
        final PinnableCursorClip selectedClip = null;

        LauncherRetriggerActions.retriggerCurrentClip(selectedClip);
    }

    @Test
    void globalRetriggerDoesNotTouchSelectedClipFallback() {
        final Application application = mock(Application.class);
        final Action action = mock(Action.class);
        final PinnableCursorClip selectedClip = mock(PinnableCursorClip.class);
        when(application.getAction("retrigger_playing_launcher_clips")).thenReturn(action);

        assertTrue(LauncherRetriggerActions.retriggerPlayingLauncherClips(application));

        verifyNoInteractions(selectedClip);
    }

    private static Action action(final String id, final String name, final String menuItemText) {
        final Action action = mock(Action.class);
        when(action.getId()).thenReturn(id);
        when(action.getName()).thenReturn(name);
        when(action.getMenuItemText()).thenReturn(menuItemText);
        return action;
    }
}
