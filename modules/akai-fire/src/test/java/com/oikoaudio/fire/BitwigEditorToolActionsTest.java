package com.oikoaudio.fire;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bitwig.extension.controller.api.Action;
import com.bitwig.extension.controller.api.Application;
import org.junit.jupiter.api.Test;

class BitwigEditorToolActionsTest {
    @Test
    void resolvesExactActionLabel() {
        final Application application = mock(Application.class);
        final Action action = mock(Action.class);
        when(application.getAction("Step Input tool")).thenReturn(action);

        assertSame(action, BitwigEditorToolActions.resolve(application, "Step Input tool"));
    }

    @Test
    void resolvesNormalizedActionId() {
        final Application application = mock(Application.class);
        final Action action = mock(Action.class);
        when(application.getAction("move_time_selection_to_first_item")).thenReturn(action);

        assertSame(
                action,
                BitwigEditorToolActions.resolve(application, "Move Time Selection to First Item"));
    }

    @Test
    void scansActionNamesAndMenuText() {
        final Application application = mock(Application.class);
        final Action other = action("other_action", "Other", "Other");
        final Action target = action("unknown", "Editor: Pointer Tool", "");
        when(application.getActions()).thenReturn(new Action[] {other, target});

        assertSame(target, BitwigEditorToolActions.resolve(application, "Pointer tool"));
    }

    @Test
    void resolvesClipEditorFocusActionVariant() {
        final Application application = mock(Application.class);
        final Action target = action("focus_clip_editor_panel", "Focus Clip Editor Panel", "");
        when(application.getActions()).thenReturn(new Action[] {target});

        assertTrue(BitwigEditorToolActions.focusClipEditorPanel(application));

        verify(target).invoke();
    }

    @Test
    void resolvesTrackHeaderFocusActionVariant() {
        final Application application = mock(Application.class);
        final Action target = action("focus_track_header", "Focus Track Header", "");
        when(application.getActions()).thenReturn(new Action[] {target});

        assertTrue(BitwigEditorToolActions.focusTrackHeader(application));

        verify(target).invoke();
    }

    @Test
    void invokesResolvedAction() {
        final Application application = mock(Application.class);
        final Action action = mock(Action.class);
        when(application.getAction("Step Input tool")).thenReturn(action);

        assertTrue(BitwigEditorToolActions.activateStepInputTool(application));

        verify(action).invoke();
    }

    @Test
    void reportsUnavailableWhenMissing() {
        final Application application = mock(Application.class);
        when(application.getActions()).thenReturn(new Action[0]);

        assertFalse(BitwigEditorToolActions.activateStepInputTool(application));
    }

    private static Action action(final String id, final String name, final String menuItemText) {
        final Action action = mock(Action.class);
        when(action.getId()).thenReturn(id);
        when(action.getName()).thenReturn(name);
        when(action.getMenuItemText()).thenReturn(menuItemText);
        return action;
    }
}
