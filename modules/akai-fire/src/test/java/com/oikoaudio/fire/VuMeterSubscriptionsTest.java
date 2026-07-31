package com.oikoaudio.fire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bitwig.extension.controller.api.Subscribable;
import org.junit.jupiter.api.Test;

class VuMeterSubscriptionsTest {
    @Test
    void activeGroupOwnsSubscriptionsThatItReleasesOnDeactivation() {
        final VuMeterSubscriptions subscriptions = new VuMeterSubscriptions();
        final StrictSubscribable container = new StrictSubscribable();
        final StrictSubscribable source = new StrictSubscribable();
        final VuMeterSubscriptions.Group group = subscriptions.createGroup(container);
        group.register(source);
        group.select(0);
        group.setActive(true);

        subscriptions.start();
        assertEquals(1, container.count);
        assertEquals(1, source.count);

        group.setActive(false);
        assertEquals(0, container.count);
        assertEquals(0, source.count);
    }

    @Test
    void registrationDoesNotTouchHostSubscriptionsBeforeStartupReconciliation() {
        final VuMeterSubscriptions subscriptions = new VuMeterSubscriptions();
        final Subscribable selectedSource = mock(Subscribable.class);
        final Subscribable container = mock(Subscribable.class);
        final Subscribable groupedSource = mock(Subscribable.class);

        final VuMeterSubscriptions.SelectedSource selected =
                subscriptions.registerSelected(selectedSource);
        final VuMeterSubscriptions.Group group = subscriptions.createGroup(container);
        group.register(groupedSource);
        selected.setActive(true);
        group.select(0);
        group.setActive(true);

        verifyNoInteractions(selectedSource, container, groupedSource);
    }

    @Test
    void selectedSourceTracksOffSelectedAndAllModes() {
        final VuMeterSubscriptions subscriptions = new VuMeterSubscriptions();
        final Subscribable source = mock(Subscribable.class);
        when(source.isSubscribed()).thenReturn(true);
        final VuMeterSubscriptions.SelectedSource selected = subscriptions.registerSelected(source);
        subscriptions.start();
        verify(source).unsubscribe();
        clearInvocations(source);

        selected.setActive(true);
        verify(source).subscribe();
        clearInvocations(source);

        subscriptions.setMode(VuMeterMode.OFF);
        subscriptions.setMode(VuMeterMode.OFF);
        verify(source).unsubscribe();

        subscriptions.setMode(VuMeterMode.SELECTED);
        subscriptions.setMode(VuMeterMode.ALL);
        verify(source).subscribe();
        assertEquals(VuMeterMode.ALL, subscriptions.mode());
    }

    @Test
    void indexedGroupSubscribesOnlyItsSelectionUntilAllMode() {
        final VuMeterSubscriptions subscriptions = new VuMeterSubscriptions();
        final Subscribable container = mock(Subscribable.class);
        final Subscribable first = mock(Subscribable.class);
        final Subscribable second = mock(Subscribable.class);
        when(container.isSubscribed()).thenReturn(true);
        when(first.isSubscribed()).thenReturn(true);
        when(second.isSubscribed()).thenReturn(true);
        final VuMeterSubscriptions.Group group = subscriptions.createGroup(container);
        group.register(first);
        group.register(second);
        subscriptions.start();
        verify(container).unsubscribe();
        verify(first).unsubscribe();
        verify(second).unsubscribe();
        clearInvocations(first, second, container);

        group.select(1);
        verify(second, never()).subscribe();
        group.setActive(true);
        verify(container).subscribe();
        verify(second).subscribe();
        verify(first, never()).subscribe();
        clearInvocations(first, second);

        subscriptions.setMode(VuMeterMode.ALL);
        verify(first).subscribe();
        verify(second, never()).subscribe();
        clearInvocations(first, second);

        subscriptions.setMode(VuMeterMode.SELECTED);
        verify(first).unsubscribe();
        verify(second, never()).unsubscribe();

        subscriptions.setMode(VuMeterMode.OFF);
        verify(container).unsubscribe();
        verify(second).unsubscribe();
    }

    @Test
    void leavingOffResetsCachedMeterStateOnce() {
        final VuMeterSubscriptions subscriptions = new VuMeterSubscriptions();
        final Runnable reset = mock(Runnable.class);
        subscriptions.onReEnabled(reset);
        subscriptions.start();

        subscriptions.setMode(VuMeterMode.OFF);
        subscriptions.setMode(VuMeterMode.SELECTED);
        subscriptions.setMode(VuMeterMode.ALL);

        verify(reset, times(1)).run();
    }

    private static final class StrictSubscribable implements Subscribable {
        private int count;

        @Override
        public boolean isSubscribed() {
            return count > 0;
        }

        @Override
        @SuppressWarnings("deprecation")
        public void setIsSubscribed(final boolean value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void subscribe() {
            count++;
        }

        @Override
        public void unsubscribe() {
            if (count == 0) {
                throw new IllegalStateException("unsubscribe called more times than subscribe");
            }
            count--;
        }
    }
}
