package com.oikoaudio.fire;

import com.bitwig.extension.controller.api.Action;
import com.bitwig.extension.controller.api.Application;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class BitwigEditorToolActions {
    static final String STEP_INPUT_TOOL_LABEL = "Step Input tool";
    static final String POINTER_TOOL_LABEL = "Pointer tool";
    static final String MOVE_TIME_SELECTION_TO_FIRST_ITEM_LABEL =
            "Move Time Selection to First Item";
    private static final String[] FOCUS_TRACK_HEADER_LABELS = {
        "Focus Track Header", "Focus Selected Track Header", "Focus/toggle Track Header"
    };
    private static final String[] FOCUS_CLIP_EDITOR_PANEL_LABELS = {
        "Focus/toggle Clip Editor Panel",
        "Focus Clip Editor Panel",
        "Toggle Clip Editor Panel",
        "Focus or Toggle Clip Editor Panel"
    };

    private BitwigEditorToolActions() {}

    public static boolean activateStepInputTool(final Application application) {
        return invoke(application, STEP_INPUT_TOOL_LABEL);
    }

    public static boolean activatePointerTool(final Application application) {
        return invoke(application, POINTER_TOOL_LABEL);
    }

    public static boolean moveTimeSelectionToFirstItem(final Application application) {
        return invoke(application, MOVE_TIME_SELECTION_TO_FIRST_ITEM_LABEL);
    }

    public static boolean focusClipEditorPanel(final Application application) {
        for (final String label : FOCUS_CLIP_EDITOR_PANEL_LABELS) {
            if (invoke(application, label)) {
                return true;
            }
        }
        return false;
    }

    public static boolean focusTrackHeader(final Application application) {
        for (final String label : FOCUS_TRACK_HEADER_LABELS) {
            if (invoke(application, label)) {
                return true;
            }
        }
        return false;
    }

    public static boolean invoke(final Application application, final String actionLabel) {
        final Action action = resolve(application, actionLabel);
        if (action == null) {
            return false;
        }
        action.invoke();
        return true;
    }

    static Action resolve(final Application application, final String actionLabel) {
        if (application == null || actionLabel == null || actionLabel.isBlank()) {
            return null;
        }
        Action action = application.getAction(actionLabel);
        if (action != null) {
            return action;
        }
        action = application.getAction(normalize(actionLabel).replace(' ', '_'));
        if (action != null) {
            return action;
        }
        action = application.getAction(normalize(actionLabel).replace(' ', '-'));
        if (action != null) {
            return action;
        }
        final Action[] actions = application.getActions();
        if (actions == null) {
            return null;
        }
        final String normalizedLabel = normalize(actionLabel);
        for (final Action candidate : actions) {
            if (matches(candidate, normalizedLabel)) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean matches(final Action action, final String normalizedLabel) {
        return action != null
                && (matchesText(action.getId(), normalizedLabel)
                        || matchesText(action.getName(), normalizedLabel)
                        || matchesText(action.getMenuItemText(), normalizedLabel));
    }

    private static boolean matchesText(final String text, final String normalizedLabel) {
        final String normalized = normalize(text);
        return normalized.equals(normalizedLabel)
                || normalized.contains(normalizedLabel)
                || importantWords(normalizedLabel).stream().allMatch(normalized::contains);
    }

    public static void logCandidateActions(
            final Application application, final Consumer<String> logger) {
        if (application == null || logger == null) {
            return;
        }
        final Action[] actions = application.getActions();
        if (actions == null) {
            logger.accept("Bitwig editor action scan: no actions exposed");
            return;
        }
        logger.accept(
                "Bitwig editor action scan: candidates containing step/input/pointer/time/selection/first/focus/toggle/clip/editor/panel");
        for (final Action action : actions) {
            if (action != null && isLikelyEditorAction(action)) {
                logger.accept(
                        "  id='%s' name='%s' menu='%s'"
                                .formatted(
                                        action.getId(),
                                        action.getName(),
                                        action.getMenuItemText()));
            }
        }
    }

    private static boolean isLikelyEditorAction(final Action action) {
        final String combined =
                normalize(
                        "%s %s %s"
                                .formatted(
                                        action.getId(),
                                        action.getName(),
                                        action.getMenuItemText()));
        return Stream.of(
                        "step",
                        "input",
                        "pointer",
                        "time",
                        "selection",
                        "first",
                        "focus",
                        "toggle",
                        "clip",
                        "editor",
                        "panel")
                .anyMatch(combined::contains);
    }

    private static Set<String> importantWords(final String normalizedLabel) {
        return Stream.of(normalizedLabel.split(" "))
                .filter(word -> !word.isBlank())
                .filter(word -> !"tool".equals(word))
                .filter(word -> !"to".equals(word))
                .collect(Collectors.toSet());
    }

    private static String normalize(final String text) {
        if (text == null) {
            return "";
        }
        return text.trim()
                .replace('_', ' ')
                .replace('-', ' ')
                .replace('/', ' ')
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }
}
