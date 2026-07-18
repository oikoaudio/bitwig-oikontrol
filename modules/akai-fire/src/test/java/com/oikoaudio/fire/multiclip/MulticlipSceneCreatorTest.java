package com.oikoaudio.fire.multiclip;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.Project;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class MulticlipSceneCreatorTest {
    @Test
    void appendsProjectScenesUntilTheRequestedAbsoluteSceneExists() {
        final ControllerHost host = mock(ControllerHost.class);
        final Project project = mock(Project.class);
        final Deque<Runnable> tasks = new ArrayDeque<>();
        final AtomicInteger sceneCount = new AtomicInteger(1);
        final AtomicBoolean completed = new AtomicBoolean();
        Mockito.doAnswer(
                        invocation -> {
                            tasks.addLast(invocation.getArgument(0));
                            return null;
                        })
                .when(host)
                .scheduleTask(any(Runnable.class), anyLong());
        Mockito.doAnswer(
                        invocation -> {
                            sceneCount.incrementAndGet();
                            return null;
                        })
                .when(project)
                .createScene();
        final MulticlipSceneCreator creator =
                new MulticlipSceneCreator(host, project, sceneCount::get);

        creator.ensureExists(2, completed::set);
        while (!tasks.isEmpty()) {
            tasks.removeFirst().run();
        }

        assertTrue(completed.get());
        verify(project, times(2)).createScene();
    }
}
