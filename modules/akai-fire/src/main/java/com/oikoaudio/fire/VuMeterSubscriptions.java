package com.oikoaudio.fire;

import com.bitwig.extension.controller.api.Subscribable;
import java.util.ArrayList;
import java.util.List;

/** Controls the Bitwig objects used exclusively as VU-meter sources. */
public final class VuMeterSubscriptions {
    private final List<SelectedSource> selectedSources = new ArrayList<>();
    private final List<Group> groups = new ArrayList<>();
    private final List<Runnable> reEnabledActions = new ArrayList<>();
    private VuMeterMode mode = VuMeterMode.SELECTED;
    private boolean started;

    public SelectedSource registerSelected(final Subscribable source) {
        final SelectedSource selectedSource = new SelectedSource(source);
        selectedSources.add(selectedSource);
        if (started) {
            selectedSource.start();
        }
        return selectedSource;
    }

    public Group createGroup(final Subscribable container) {
        final Group group = new Group(container);
        groups.add(group);
        if (started) {
            group.start();
        }
        return group;
    }

    /** Reconciles subscriptions after Bitwig has completed extension initialization. */
    public void start() {
        if (started) {
            return;
        }
        started = true;
        selectedSources.forEach(SelectedSource::start);
        groups.forEach(Group::start);
    }

    public void setMode(final VuMeterMode mode) {
        final VuMeterMode normalized = mode == null ? VuMeterMode.SELECTED : mode;
        if (this.mode == normalized) {
            return;
        }
        final VuMeterMode previous = this.mode;
        this.mode = normalized;
        if (!started) {
            return;
        }
        selectedSources.forEach(SelectedSource::reconcile);
        groups.forEach(Group::reconcile);
        if (previous == VuMeterMode.OFF && normalized != VuMeterMode.OFF) {
            reEnabledActions.forEach(Runnable::run);
        }
    }

    public void onReEnabled(final Runnable action) {
        reEnabledActions.add(action);
    }

    public VuMeterMode mode() {
        return mode;
    }

    public final class SelectedSource {
        private final Entry source;
        private boolean active;

        private SelectedSource(final Subscribable source) {
            this.source = new Entry(source);
        }

        public void setActive(final boolean active) {
            if (this.active == active) {
                return;
            }
            this.active = active;
            if (started) {
                reconcile();
            }
        }

        private void reconcile() {
            source.setSubscribed(shouldSubscribe());
        }

        private void start() {
            source.start(shouldSubscribe());
        }

        private boolean shouldSubscribe() {
            return mode == VuMeterMode.ALL || (mode == VuMeterMode.SELECTED && active);
        }
    }

    public final class Group {
        private final Entry container;
        private final List<Entry> sources = new ArrayList<>();
        private int selectedIndex = -1;
        private boolean active;

        private Group(final Subscribable container) {
            this.container = new Entry(container);
        }

        public void register(final Subscribable source) {
            final Entry entry = new Entry(source);
            sources.add(entry);
            if (started) {
                entry.start(shouldSubscribe(sources.size() - 1));
            }
        }

        public void select(final int index) {
            final int normalized = index >= 0 && index < sources.size() ? index : -1;
            if (selectedIndex == normalized) {
                return;
            }
            selectedIndex = normalized;
            if (started) {
                reconcile();
            }
        }

        public void setActive(final boolean active) {
            if (this.active == active) {
                return;
            }
            this.active = active;
            if (started) {
                reconcile();
            }
        }

        private void reconcile() {
            container.setSubscribed(shouldSubscribeContainer());
            for (int index = 0; index < sources.size(); index++) {
                reconcile(index, sources.get(index));
            }
        }

        private void start() {
            container.start(shouldSubscribeContainer());
            for (int index = 0; index < sources.size(); index++) {
                sources.get(index).start(shouldSubscribe(index));
            }
        }

        private void reconcile(final int index, final Entry entry) {
            entry.setSubscribed(shouldSubscribe(index));
        }

        private boolean shouldSubscribeContainer() {
            return mode == VuMeterMode.ALL || (mode == VuMeterMode.SELECTED && active);
        }

        private boolean shouldSubscribe(final int index) {
            return mode == VuMeterMode.ALL
                    || (mode == VuMeterMode.SELECTED && active && index == selectedIndex);
        }
    }

    private static final class Entry {
        private final Subscribable source;
        private boolean subscribed;
        private boolean managed;

        private Entry(final Subscribable source) {
            this.source = source;
        }

        private void start(final boolean subscribed) {
            if (managed) {
                setSubscribed(subscribed);
                return;
            }
            if (source.isSubscribed()) {
                source.unsubscribe();
            }
            managed = true;
            setSubscribed(subscribed);
        }

        private void setSubscribed(final boolean subscribed) {
            if (this.subscribed == subscribed) {
                return;
            }
            this.subscribed = subscribed;
            if (subscribed) {
                source.subscribe();
            } else {
                source.unsubscribe();
            }
        }
    }
}
