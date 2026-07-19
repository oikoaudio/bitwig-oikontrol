package com.oikoaudio.fire.multiclip;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.Project;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

final class MulticlipSceneCreator {
    private static final int OBSERVER_DELAY_MS = 50;
    private static final int MAX_ATTEMPTS = 64;

    private final ControllerHost host;
    private final Project project;
    private final IntSupplier sceneCount;
    private long generation;

    MulticlipSceneCreator(
            final ControllerHost host, final Project project, final IntSupplier sceneCount) {
        this.host = host;
        this.project = project;
        this.sceneCount = sceneCount;
    }

    void ensureExists(final int absoluteScene, final Consumer<Boolean> completion) {
        final long requestGeneration = ++generation;
        ensureExists(absoluteScene, completion, requestGeneration, 0);
    }

    void invalidate() {
        generation++;
    }

    private void ensureExists(
            final int absoluteScene,
            final Consumer<Boolean> completion,
            final long requestGeneration,
            final int attempts) {
        if (requestGeneration != generation) {
            return;
        }
        if (sceneCount.getAsInt() > absoluteScene) {
            completion.accept(true);
            return;
        }
        if (attempts >= MAX_ATTEMPTS) {
            completion.accept(false);
            return;
        }
        project.createScene();
        host.scheduleTask(
                () -> ensureExists(absoluteScene, completion, requestGeneration, attempts + 1),
                OBSERVER_DELAY_MS);
    }
}
