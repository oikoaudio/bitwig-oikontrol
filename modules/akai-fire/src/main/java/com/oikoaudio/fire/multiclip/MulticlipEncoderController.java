package com.oikoaudio.fire.multiclip;

import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.values.BooleanValueObject;
import com.oikoaudio.fire.AkaiFireOikontrolExtension;
import com.oikoaudio.fire.control.MixerEncoderProfile;
import com.oikoaudio.fire.control.TouchEncoder;
import com.oikoaudio.fire.display.EncoderFooterLegend;
import com.oikoaudio.fire.sequence.EncoderBank;
import com.oikoaudio.fire.sequence.EncoderBankLayout;
import com.oikoaudio.fire.sequence.EncoderMode;
import com.oikoaudio.fire.sequence.EncoderSlotBinding;
import com.oikoaudio.fire.sequence.NoteStepAccess;
import com.oikoaudio.fire.sequence.StepSequencerEncoderLayer;
import com.oikoaudio.fire.sequence.StepSequencerHost;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Reuses Drum XOX encoder pages against the active Lane Clip and group-pinned DrumPad. */
final class MulticlipEncoderController implements StepSequencerHost {
    private static final int DEFAULT_VELOCITY = 100;
    private static final int ACCENT_VELOCITY = 127;
    private static final double DEFAULT_GATE = 0.12;

    private final AkaiFireOikontrolExtension driver;
    private final MulticlipClipController clips;
    private final MulticlipPadInteractionState pads;
    private final MulticlipDrumPadEncoderController drumPad;
    private final Context context;
    private final BooleanValueObject lengthDisplay = new BooleanValueObject();
    private final BooleanValueObject deleteHeld = new BooleanValueObject();
    private final Map<Target, MulticlipEuclidState> euclidStates = new HashMap<>();
    private final EncoderBankLayout layout;
    private final StepSequencerEncoderLayer encoderLayer;

    private int defaultVelocity = DEFAULT_VELOCITY;
    private double defaultPressure;
    private double defaultTimbre;

    MulticlipEncoderController(
            final AkaiFireOikontrolExtension driver,
            final MulticlipClipController clips,
            final MulticlipPadInteractionState pads,
            final MulticlipDrumPadEncoderController drumPad,
            final Context context) {
        this.driver = driver;
        this.clips = clips;
        this.pads = pads;
        this.drumPad = drumPad;
        this.context = context;
        layout = createLayout();
        encoderLayer = new StepSequencerEncoderLayer(this, driver, layout);
    }

    void activate() {
        encoderLayer.activate();
    }

    void deactivate() {
        encoderLayer.deactivate();
        lengthDisplay.set(false);
        deleteHeld.set(false);
    }

    void setDeleteHeld(final boolean held) {
        deleteHeld.set(held);
    }

    int insertionVelocity() {
        return defaultVelocity;
    }

    MulticlipNoteDefaults insertionDefaults() {
        return new MulticlipNoteDefaults(defaultPressure, defaultTimbre);
    }

    private EncoderBankLayout createLayout() {
        final Map<EncoderMode, EncoderBank> banks = new EnumMap<>(EncoderMode.class);
        banks.put(
                EncoderMode.CHANNEL,
                new EncoderBank(
                        modeInfo(EncoderMode.CHANNEL),
                        footer(EncoderMode.CHANNEL),
                        new EncoderSlotBinding[] {
                            noteAccessSlot(NoteStepAccess.DURATION, null),
                            noteAccessSlot(NoteStepAccess.CHANCE, null),
                            noteAccessSlot(NoteStepAccess.VELOCITY_SPREAD, null),
                            noteAccessSlot(NoteStepAccess.REPEATS, null)
                        }));
        banks.put(
                EncoderMode.MIXER,
                new EncoderBank(
                        modeInfo(EncoderMode.MIXER),
                        footer(EncoderMode.MIXER),
                        new EncoderSlotBinding[] {
                            mixerSlot(0), mixerSlot(1), mixerSlot(2), mixerSlot(3)
                        }));
        banks.put(
                EncoderMode.USER_1,
                new EncoderBank(
                        modeInfo(EncoderMode.USER_1),
                        footer(EncoderMode.USER_1),
                        new EncoderSlotBinding[] {
                            noteAccessSlot(NoteStepAccess.VELOCITY, velocityDefault()),
                            noteAccessSlot(NoteStepAccess.PRESSURE, pressureDefault()),
                            noteAccessSlot(NoteStepAccess.TIMBRE, timbreDefault()),
                            noteAccessSlot(NoteStepAccess.PITCH, null)
                        }));
        banks.put(
                EncoderMode.USER_2,
                new EncoderBank(
                        modeInfo(EncoderMode.USER_2),
                        footer(EncoderMode.USER_2),
                        new EncoderSlotBinding[] {
                            euclidSlot(0), euclidSlot(1), euclidSlot(2), euclidSlot(3)
                        }));
        return new EncoderBankLayout(banks);
    }

    static String modeInfo(final EncoderMode mode) {
        return switch (mode) {
            case CHANNEL -> "1: Note Length\n2: Chance\n3: Vel Spread\n4: Repeat";
            case MIXER -> "1: Volume\n2: Pan\n3: Send 1\n4: Send 2";
            case USER_1 -> "1: Velocity\n2: Pressure\n3: Timbre\n4: Pitch Expr";
            case USER_2 -> "1: Euclid Len\n2: Euclid Pulses\n3: Euclid Rotation\n4: Accent Density";
        };
    }

    static String footer(final EncoderMode mode) {
        return switch (mode) {
            case CHANNEL -> EncoderFooterLegend.of("Len", "Chnc", "VSpr", "Rpt");
            case MIXER -> EncoderFooterLegend.of("Vol", "Pan", "S1", "S2");
            case USER_1 -> EncoderFooterLegend.of("Velo", "Pres", "Timb", "PExp");
            case USER_2 -> EncoderFooterLegend.of("ELen", "EPul", "ERot", "ADen");
        };
    }

    private EncoderSlotBinding noteAccessSlot(
            final NoteStepAccess access,
            final StepSequencerEncoderLayer.EmptyNoteAccessHandler emptyHandler) {
        return new EncoderSlotBinding() {
            @Override
            public double stepSize() {
                return access.getResolution();
            }

            @Override
            public void bind(
                    final StepSequencerEncoderLayer handler,
                    final Layer layer,
                    final TouchEncoder encoder,
                    final int slotIndex) {
                handler.bindNoteAccess(layer, encoder, slotIndex, access, emptyHandler);
            }
        };
    }

    private EncoderSlotBinding mixerSlot(final int index) {
        return new EncoderSlotBinding() {
            @Override
            public double stepSize() {
                return MixerEncoderProfile.STEP_SIZE;
            }

            @Override
            public void bind(
                    final StepSequencerEncoderLayer handler,
                    final Layer layer,
                    final TouchEncoder encoder,
                    final int slotIndex) {
                encoder.bindContinuousEncoder(
                        layer,
                        driver::isGlobalShiftHeld,
                        increment ->
                                drumPad.adjustMixer(index, driver.isGlobalShiftHeld(), increment));
                encoder.bindTouched(
                        layer,
                        touched -> {
                            if (!touched) {
                                driver.getOled().clearScreenDelayed();
                                return;
                            }
                            final boolean resettable =
                                    index != 0 && drumPad.hasMixerParameter(index);
                            final String resetHelp =
                                    index == 0
                                            ? "No reset"
                                            : drumPad.hasMixerParameter(index)
                                                    ? "No reset"
                                                    : "Unmapped";
                            if (driver.handleKnobModeEncoderReset(
                                    true,
                                    resettable,
                                    mixerLabel(index),
                                    resetHelp,
                                    () -> drumPad.resetMixer(index),
                                    () -> drumPad.showMixer(index))) {
                                return;
                            }
                            drumPad.showMixer(index);
                        });
            }
        };
    }

    private EncoderSlotBinding euclidSlot(final int index) {
        return new EncoderSlotBinding() {
            @Override
            public double stepSize() {
                return index < 2 ? 0.1 : 0.25;
            }

            @Override
            public void bind(
                    final StepSequencerEncoderLayer handler,
                    final Layer layer,
                    final TouchEncoder encoder,
                    final int slotIndex) {
                encoder.bindThresholdedEncoder(
                        layer,
                        5,
                        10,
                        driver::isGlobalShiftHeld,
                        increment -> adjustEuclid(index, increment));
                encoder.bindTouched(layer, touched -> handleEuclidTouch(index, touched));
            }
        };
    }

    private void adjustEuclid(final int index, final int increment) {
        if (!context.activeClipExists()) {
            driver.getOled().valueInfo("Euclid", "Empty Lane Clip");
            return;
        }
        final Target target = target();
        final MulticlipEuclidState current =
                euclidStates.getOrDefault(target, MulticlipEuclidState.defaults());
        final MulticlipEuclidState adjusted = current.adjusted(index, increment);
        euclidStates.put(target, adjusted);
        applyEuclid(adjusted);
        showEuclid(index, adjusted);
    }

    private void handleEuclidTouch(final int index, final boolean touched) {
        if (!touched) {
            driver.getOled().clearScreenDelayed();
            return;
        }
        final Target target = target();
        final MulticlipEuclidState current =
                euclidStates.getOrDefault(target, MulticlipEuclidState.defaults());
        if (driver.handleKnobModeEncoderReset(
                true,
                true,
                euclidLabel(index),
                "No reset",
                () -> {
                    final MulticlipEuclidState reset = resetEuclid(current, index);
                    euclidStates.put(target, reset);
                    applyEuclid(reset);
                },
                () -> showEuclid(index, current))) {
            return;
        }
        showEuclid(index, current);
    }

    private MulticlipEuclidState resetEuclid(final MulticlipEuclidState state, final int index) {
        final MulticlipEuclidState defaults = MulticlipEuclidState.defaults();
        final int delta =
                switch (index) {
                    case 0 -> defaults.length() - state.length();
                    case 1 -> defaults.pulses() - state.pulses();
                    case 2 -> defaults.rotation() - state.rotation();
                    default -> defaults.accentPulses() - state.accentPulses();
                };
        return state.adjusted(index, delta);
    }

    private void applyEuclid(final MulticlipEuclidState state) {
        final int visibleSteps =
                MulticlipTiming.visibleLoopStepCount(
                        clips.loopLength(),
                        context.firstVisibleStep(),
                        MulticlipXoxLayout.PATTERN_COUNT);
        if (visibleSteps <= 0) {
            return;
        }
        for (int step = 0; step < visibleSteps; step++) {
            for (final int channel : clips.channelsAt(step)) {
                clips.clearStep(channel, step);
            }
        }
        final boolean[] pattern = state.pattern();
        final boolean[] accents = accentPattern(pattern, state.accentPulses());
        for (int step = 0; step < visibleSteps; step++) {
            final int patternStep = step % pattern.length;
            if (pattern[patternStep]) {
                clips.setStep(
                        context.midiChannel(),
                        step,
                        accents[patternStep] ? ACCENT_VELOCITY : defaultVelocity,
                        DEFAULT_GATE,
                        insertionDefaults());
            }
        }
        driver.getOled().detailInfo("EUCLID", "Applied Active Lane Clip");
    }

    private static boolean[] accentPattern(final boolean[] pattern, final int accentPulses) {
        final List<Integer> hits = new ArrayList<>();
        for (int step = 0; step < pattern.length; step++) {
            if (pattern[step]) {
                hits.add(step);
            }
        }
        final boolean[] accents = new boolean[pattern.length];
        final int accentCount = Math.min(accentPulses, hits.size());
        for (int index = 0; index < hits.size(); index++) {
            if (accentCount > 0 && (index * accentCount) % hits.size() < accentCount) {
                accents[hits.get(index)] = true;
            }
        }
        return accents;
    }

    private void showEuclid(final int index, final MulticlipEuclidState state) {
        final int value =
                switch (index) {
                    case 0 -> state.length();
                    case 1 -> state.pulses();
                    case 2 -> state.rotation();
                    default -> state.accentPulses();
                };
        driver.getOled().valueInfo(euclidLabel(index), Integer.toString(value));
    }

    private StepSequencerEncoderLayer.EmptyNoteAccessHandler velocityDefault() {
        return new EmptyDefault(
                () -> Integer.toString(defaultVelocity),
                amount -> defaultVelocity = clamp(defaultVelocity + amount, 1, 127),
                () -> defaultVelocity = DEFAULT_VELOCITY,
                "Velocity");
    }

    private StepSequencerEncoderLayer.EmptyNoteAccessHandler pressureDefault() {
        return new EmptyDefault(
                () -> percent(defaultPressure),
                amount -> defaultPressure = clamp(defaultPressure + amount * 0.01, 0.0, 1.0),
                () -> defaultPressure = 0.0,
                "Pressure");
    }

    private StepSequencerEncoderLayer.EmptyNoteAccessHandler timbreDefault() {
        return new EmptyDefault(
                () -> percent(defaultTimbre),
                amount -> defaultTimbre = clamp(defaultTimbre + amount * 0.01, -1.0, 1.0),
                () -> defaultTimbre = 0.0,
                "Timbre");
    }

    private Target target() {
        return new Target(context.activeChildPosition(), context.activeScene());
    }

    private static String mixerLabel(final int index) {
        return switch (index) {
            case 0 -> "Volume";
            case 1 -> "Pan";
            case 2 -> "Send 1";
            default -> "Send 2";
        };
    }

    private static String euclidLabel(final int index) {
        return switch (index) {
            case 0 -> "Euclid Length";
            case 1 -> "Euclid Pulses";
            case 2 -> "Euclid Rotation";
            default -> "Accent Density";
        };
    }

    private static String percent(final double value) {
        return Math.round(value * 100.0) + "%";
    }

    private static int clamp(final int value, final int minimum, final int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static double clamp(final double value, final double minimum, final double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    @Override
    public boolean isSelectHeld() {
        return context.selectHeld();
    }

    @Override
    public CursorRemoteControlsPage getActiveRemoteControlsPage() {
        return drumPad.activeRemoteControlsPage();
    }

    @Override
    public boolean isPadBeingHeld() {
        return false;
    }

    @Override
    public List<NoteStep> getOnNotes() {
        return clips.allNotes();
    }

    @Override
    public List<NoteStep> getHeldNotes() {
        final List<NoteStep> held = new ArrayList<>();
        for (int pad = MulticlipXoxLayout.PATTERN_START; pad < 64; pad++) {
            if (pads.isHeld(pad)) {
                held.addAll(clips.notesAt(MulticlipXoxLayout.visibleStep(pad)));
            }
        }
        return List.copyOf(held);
    }

    @Override
    public String getDetails(final List<NoteStep> heldNotes) {
        return context.activeLaneName() + " <" + heldNotes.size() + ">";
    }

    @Override
    public double getGridResolution() {
        return MulticlipTiming.STEP_BEATS;
    }

    @Override
    public BooleanValueObject getLengthDisplay() {
        return lengthDisplay;
    }

    @Override
    public BooleanValueObject getDeleteHeld() {
        return deleteHeld;
    }

    @Override
    public String getPadInfo() {
        return context.activeLaneName();
    }

    @Override
    public void exitRecurrenceEdit() {}

    @Override
    public void enterRecurrenceEdit(final List<NoteStep> notes) {}

    @Override
    public void updateRecurrenceLength(final int length) {}

    @Override
    public void registerModifiedSteps(final List<NoteStep> notes) {
        pads.consumeHeldPattern();
    }

    @Override
    public EncoderBankLayout getEncoderBankLayout() {
        return layout;
    }

    @Override
    public int getDefaultStepVelocity() {
        return defaultVelocity;
    }

    @Override
    public double getDefaultStepPressure() {
        return defaultPressure;
    }

    @Override
    public double getDefaultStepDuration() {
        return DEFAULT_GATE;
    }

    private record Target(int childPosition, int scene) {}

    private final class EmptyDefault implements StepSequencerEncoderLayer.EmptyNoteAccessHandler {
        private final java.util.function.Supplier<String> value;
        private final java.util.function.IntConsumer adjust;
        private final Runnable reset;
        private final String label;

        private EmptyDefault(
                final java.util.function.Supplier<String> value,
                final java.util.function.IntConsumer adjust,
                final Runnable reset,
                final String label) {
            this.value = value;
            this.adjust = adjust;
            this.reset = reset;
            this.label = label;
        }

        @Override
        public void adjust(final int amount) {
            adjust.accept(amount);
            show();
        }

        @Override
        public void show() {
            driver.getOled().valueInfo(label + " Default", value.get());
        }

        @Override
        public void reset() {
            reset.run();
            show();
        }
    }

    interface Context {
        boolean selectHeld();

        String activeLaneName();

        int activeChildPosition();

        int activeScene();

        int firstVisibleStep();

        int midiChannel();

        boolean activeClipExists();
    }
}
