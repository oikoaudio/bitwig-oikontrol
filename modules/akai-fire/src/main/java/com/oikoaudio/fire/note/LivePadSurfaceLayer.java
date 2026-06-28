package com.oikoaudio.fire.note;

import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.CursorDeviceFollowMode;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.DrumPad;
import com.bitwig.extension.controller.api.DrumPadBank;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.PinnableCursorClip;
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.MusicalScale;
import com.bitwig.extensions.framework.values.Midi;
import com.oikoaudio.fire.AkaiFireOikontrolExtension;
import com.oikoaudio.fire.BitwigEditorToolActions;
import com.oikoaudio.fire.ColorLookup;
import com.oikoaudio.fire.FireControlPreferences;
import com.oikoaudio.fire.NoteAssign;
import com.oikoaudio.fire.control.BiColorButton;
import com.oikoaudio.fire.control.ContinuousEncoderScaler;
import com.oikoaudio.fire.control.EncoderTurnBehavior;
import com.oikoaudio.fire.control.EncoderTouchDisplayHandler;
import com.oikoaudio.fire.control.EncoderStepAccumulator;
import com.oikoaudio.fire.control.EncoderValueProfile;
import com.oikoaudio.fire.control.MixerEncoderProfile;
import com.oikoaudio.fire.control.ModeButtonLights;
import com.oikoaudio.fire.control.PadBankRowControlBindings;
import com.oikoaudio.fire.control.ParameterEncoderBinding;
import com.oikoaudio.fire.control.RelativeEncoderMagnitude;
import com.oikoaudio.fire.control.TouchEncoder;
import com.oikoaudio.fire.control.VelocitySettings;
import com.oikoaudio.fire.display.EncoderFooterLegend;
import com.oikoaudio.fire.display.OledDisplay;
import com.oikoaudio.fire.display.PeakRmsOledView;
import com.oikoaudio.fire.display.VuMeterFormatter;
import com.oikoaudio.fire.lights.BiColorLightState;
import com.oikoaudio.fire.lights.RgbLigthState;
import com.oikoaudio.fire.music.SharedPitchContextController;
import com.oikoaudio.fire.utils.PatternButtons;
import com.oikoaudio.fire.sequence.EncoderMode;
import com.oikoaudio.fire.sequence.NoteRepeatHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class LivePadSurfaceLayer extends Layer {
    private static final int CLIP_ROW_PAD_COUNT = 16;
    private static final int MIN_OCTAVE = 0;
    private static final int MAX_OCTAVE = 7;
    private static final int MIN_TRANSPOSE = 0;
    private static final int MAX_TRANSPOSE = MAX_OCTAVE * 12 + 11;
    private static final int HELD_NOTE_VELOCITY = 100;
    private static final double MIN_GATE_RATIO = 0.25;
    private static final double MAX_GATE_RATIO = 1.0;
    private static final int LIVE_NOTE_ENCODER_THRESHOLD = 4;
    private static final int LIVE_LAYOUT_ENCODER_THRESHOLD = 6;
    private static final int LIVE_VELOCITY_ENCODER_THRESHOLD = 1;
    private static final int LIVE_PITCH_OFFSET_ENCODER_THRESHOLD = 3;
    private static final int MIN_MIDI_VALUE = 0;
    private static final int MAX_MIDI_VALUE = 127;
    private static final int MIN_VELOCITY = 1;
    private static final int DEFAULT_DRUM_MACHINE_LOW_NOTE = 36;
    private static final int MAX_DRUM_MACHINE_SCROLL_POSITION = MAX_MIDI_VALUE - DrumMachinePadLayout.PAD_WINDOW_SIZE + 1;
    private static final int DEFAULT_LIVE_PITCH_BEND = 64;
    private static final int DEFAULT_LIVE_PITCH_EXPRESSION = 64;
    private static final int LIVE_PITCH_BEND_RETURN_STEP = 6;
    private static final long LIVE_PITCH_BEND_RETURN_DELAY_MS = 15L;
    private static final long LIVE_PITCH_BEND_INACTIVITY_RETURN_MS = 120L;
    private static final int MIDI_CC_MOD = 1;
    private static final int MIDI_CC_BREATH = 2;
    private static final int MIDI_CC_SUSTAIN = 64;
    private static final int MIDI_CC_SOSTENUTO = 66;
    private static final int MIDI_CC_TIMBRE = 74;
    private static final int[] LIVE_PITCH_OFFSETS = {-24, -19, -12, -7, 0, 7, 12, 19, 24};
    private static final int DEFAULT_LIVE_PITCH_OFFSET_INDEX = 4;
    private static final int DRUM_MACHINE_SCROLL_COARSE_STEPS = 16;
    private static final double STEP_INPUT_DISPLAY_STEP_SIZE_BEATS = 0.25;
    private static final int MIN_SCALE_DEGREE_GLISS = -14;
    private static final int MAX_SCALE_DEGREE_GLISS = 14;
    private static final int METER_REFRESH_TICKS = 1;
    private static final RgbLigthState ROOT_COLOR = new RgbLigthState(120, 64, 0, true);
    private static final RgbLigthState IN_SCALE_COLOR = new RgbLigthState(0, 72, 110, true);
    private static final RgbLigthState HARMONIC_BRIGHT_COLOR = new RgbLigthState(0, 72, 122, true);
    private static final RgbLigthState HARMONIC_MINOR_COLOR = new RgbLigthState(18, 48, 104, true);
    private static final RgbLigthState HARMONIC_TENSE_COLOR = new RgbLigthState(68, 48, 116, true);
    private static final RgbLigthState HARMONIC_EXOTIC_COLOR = new RgbLigthState(108, 28, 72, true);
    private static final RgbLigthState HARMONIC_SYMMETRIC_COLOR = new RgbLigthState(46, 92, 42, true);
    private static final RgbLigthState OUT_OF_SCALE_COLOR = RgbLigthState.GRAY_1;

    private final AkaiFireOikontrolExtension driver;
    private final OledDisplay oled;
    private final NoteInput noteInput;
    private final NoteRepeatHandler noteRepeatHandler;
    private final SharedPitchContextController pitchContext;
    private final Integer[] noteTranslationTable = new Integer[128];
    private final NotePlayController notePlayController;
    private final NoteLiveControlSurface liveControls;
    private final NoteLivePadPerformer livePadPerformer;
    private final NoteLiveExpressionControls liveExpressionControls;
    private final CursorTrack cursorTrack;
    private final PinnableCursorClip stepInputCursorClip;
    private final PinnableCursorDevice liveCursorDevice;
    private final PinnableCursorDevice liveDrumMachineDevice;
    private final DrumPadBank liveDrumPadBank;
    private final CursorRemoteControlsPage liveRemoteControlsPage;
    private final Layer liveModeControlLayer;
    private final Layer liveChannelLayer;
    private final Layer liveMixerLayer;
    private final Layer liveUser1Layer;
    private final Layer liveUser2Layer;
    private final EncoderTouchDisplayHandler encoderTouchDisplayHandler;

    private enum LiveNoteSubMode {
        MELODIC("Note"),
        HARMONIC("Harmonic"),
        DRUM_PADS("Drum Pads");

        private final String displayName;

        LiveNoteSubMode(final String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }

        public LiveNoteSubMode next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    private enum LivePitchGlissMode {
        FIFTH_OCTAVE("5th/8v"),
        SCALE_DEGREE("ScaleDeg");

        private final String displayName;

        LivePitchGlissMode(final String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }
    }

    private boolean inKey = false;
    private boolean mainEncoderPressConsumed = false;
    private final boolean drumPadsOnly;
    private LiveNoteSubMode liveNoteSubMode = LiveNoteSubMode.MELODIC;
    private final VelocitySettings liveVelocity;
    private int livePitchOffsetIndex = DEFAULT_LIVE_PITCH_OFFSET_INDEX;
    private int liveScaleDegreeGlissOffset = 0;
    private LivePitchGlissMode livePitchGlissMode = LivePitchGlissMode.FIFTH_OCTAVE;
    private int harmonicNoteCountIndex = 2;
    private int harmonicOctaveSpan = 1;
    private boolean harmonicBassColumns = true;
    private int drumMachineScrollPosition = 0;
    private DrumMachinePadLayout.Layout drumMachineLayout = DrumMachinePadLayout.Layout.GRID64;
    private int selectedDrumPadOffset = 0;
    private int secondaryDrumPadOffset = 1;
    private boolean drumMachineDefaultPageApplied = false;
    private final boolean[][] heldBongoPads = new boolean[2][NoteGridLayout.PAD_COUNT];
    private final int[] heldBongoPadCounts = new int[2];
    private final RgbLigthState[] drumMachinePadColors = new RgbLigthState[DrumMachinePadLayout.PAD_WINDOW_SIZE];
    private final String[] drumMachinePadNames = new String[DrumMachinePadLayout.PAD_WINDOW_SIZE];
    private final boolean[] drumMachinePadExists = new boolean[DrumMachinePadLayout.PAD_WINDOW_SIZE];
    private RgbLigthState trackBaseColor = IN_SCALE_COLOR;
    private int livePitchBend = DEFAULT_LIVE_PITCH_BEND;
    private boolean livePitchBendTouched = false;
    private int livePitchBendReturnGeneration = 0;
    private int livePitchBendInactivityGeneration = 0;
    private int selectedTrackPeakMeter = 0;
    private int selectedTrackRmsMeter = 0;
    private int selectedTrackPeakMax = 0;
    private int selectedTrackRmsMax = 0;
    private int lastMeterDisplayBlink = Integer.MIN_VALUE;
    private boolean active = false;
    private boolean liveMeterDisplayActive = false;
    private boolean liveContextDisplayActive = false;
    private long liveContextDisplayRevision = Long.MIN_VALUE;
    private boolean stepInputHelperActive = false;
    private int stepInputActivationGeneration = 0;
    private int stepInputStepIndex = 0;
    private int stepInputHeldPadCount = 0;
    private boolean stepInputPadGesturePending = false;
    private double stepInputClipLengthBeats = 0.0;
    private final PeakRmsOledView selectedTrackMeterView;
    private final EncoderStepAccumulator liveVelocityEncoder = new EncoderStepAccumulator(LIVE_VELOCITY_ENCODER_THRESHOLD);
    private int livePitchOffsetEncoderCarry = 0;
    private boolean livePitchOffsetFirstStepPending = true;
    private final EncoderStepAccumulator liveScaleEncoder = new EncoderStepAccumulator(LIVE_NOTE_ENCODER_THRESHOLD);
    private final EncoderStepAccumulator liveOctaveEncoder = new EncoderStepAccumulator(LIVE_NOTE_ENCODER_THRESHOLD);
    private final EncoderStepAccumulator liveLayoutEncoder = new EncoderStepAccumulator(LIVE_LAYOUT_ENCODER_THRESHOLD);
    protected LivePadSurfaceLayer(final AkaiFireOikontrolExtension driver,
                                  final NoteRepeatHandler noteRepeatHandler,
                                  final String layerName) {
        this(driver, noteRepeatHandler, layerName, false);
    }

    protected LivePadSurfaceLayer(final AkaiFireOikontrolExtension driver,
                                  final NoteRepeatHandler noteRepeatHandler,
                                  final String layerName,
                                  final boolean drumPadsOnly) {
        super(driver.getLayers(), layerName);
        this.driver = driver;
        this.pitchContext = driver.getSharedPitchContextController();
        this.liveVelocity = driver.getSharedVelocitySettings();
        this.drumPadsOnly = drumPadsOnly;
        this.liveNoteSubMode = drumPadsOnly ? LiveNoteSubMode.DRUM_PADS : LiveNoteSubMode.MELODIC;
        this.oled = driver.getOled();
        this.selectedTrackMeterView = new PeakRmsOledView(oled);
        this.noteInput = driver.getNoteInput();
        this.noteRepeatHandler = noteRepeatHandler;
        this.liveModeControlLayer = new Layer(driver.getLayers(), "NOTE_MODE_LIVE_PAGE_CONTROLS");
        this.liveChannelLayer = new Layer(driver.getLayers(), "NOTE_MODE_LIVE_CHANNEL");
        this.liveMixerLayer = new Layer(driver.getLayers(), "NOTE_MODE_LIVE_MIXER");
        this.liveUser1Layer = new Layer(driver.getLayers(), "NOTE_MODE_LIVE_USER1");
        this.liveUser2Layer = new Layer(driver.getLayers(), "NOTE_MODE_LIVE_USER2");
        this.encoderTouchDisplayHandler = new EncoderTouchDisplayHandler(oled::clearScreenDelayed);
        final NoteLiveEncoderModeControls liveEncoderControls = new NoteLiveEncoderModeControls(
                liveEncoderLayer(liveChannelLayer),
                liveEncoderLayer(liveMixerLayer),
                liveEncoderLayer(liveUser1Layer),
                liveEncoderLayer(liveUser2Layer),
                this::applyLiveEncoderStepSizes,
                this::liveEncoderModeInfo);

        final ControllerHost host = driver.getHost();
        this.cursorTrack = host.createCursorTrack("NOTE_VIEW", "Note View", 8, CLIP_ROW_PAD_COUNT, true);
        this.cursorTrack.name().markInterested();
        this.cursorTrack.position().markInterested();
        this.cursorTrack.color().markInterested();
        this.cursorTrack.canHoldNoteData().markInterested();
        this.cursorTrack.name().addValueObserver(name -> resetSelectedTrackMeterMax());
        this.cursorTrack.position().addValueObserver(position -> resetSelectedTrackMeterMax());
        this.cursorTrack.color().addValueObserver((r, g, b) -> trackBaseColor = ColorLookup.getColor(r, g, b));
        this.cursorTrack.addVuMeterObserver(VuMeterFormatter.RANGE, -1, true, this::handleSelectedTrackPeakMeterChanged);
        this.cursorTrack.addVuMeterObserver(VuMeterFormatter.RANGE, -1, false, this::handleSelectedTrackRmsMeterChanged);
        trackBaseColor = ColorLookup.getColor(this.cursorTrack.color().get());
        this.stepInputCursorClip = cursorTrack.createLauncherCursorClip("NOTE_STEP_INPUT_CLIP", "Note Step Input Clip",
                CLIP_ROW_PAD_COUNT, 128);
        this.stepInputCursorClip.setStepSize(STEP_INPUT_DISPLAY_STEP_SIZE_BEATS);
        this.stepInputCursorClip.getLoopLength().markInterested();
        this.stepInputCursorClip.getLoopLength().addValueObserver(length -> {
            stepInputClipLengthBeats = length;
            if (stepInputHelperActive) {
                showStepInputDisplay();
            }
        });
        this.liveCursorDevice = cursorTrack.createCursorDevice("NOTE_LIVE_DEVICE", "Note Live Device", 8,
                CursorDeviceFollowMode.FOLLOW_SELECTION);
        this.liveDrumMachineDevice = drumPadsOnly
                ? driver.getViewControl().getPrimaryDevice()
                : cursorTrack.createCursorDevice("NOTE_DRUM_MACHINE", "Note Drum Machine", 8,
                CursorDeviceFollowMode.FIRST_INSTRUMENT);
        this.liveDrumMachineDevice.hasDrumPads().markInterested();
        this.liveDrumPadBank = liveDrumMachineDevice.createDrumPadBank(DrumMachinePadLayout.PAD_WINDOW_SIZE);
        observeLiveDrumPads();
        this.liveRemoteControlsPage = liveCursorDevice.createCursorRemoteControlsPage(8);
        this.liveRemoteControlsPage.selectedPageIndex().markInterested();
        this.liveRemoteControlsPage.pageCount().markInterested();
        this.liveRemoteControlsPage.pageNames().markInterested();
        this.liveRemoteControlsPage.getName().markInterested();
        final NoteLivePerformanceControls livePerformanceControls = new NoteLivePerformanceControls(
                value -> noteInput.sendRawMidiEvent(Midi.CC, MIDI_CC_SUSTAIN, value),
                value -> noteInput.sendRawMidiEvent(Midi.CC, MIDI_CC_SOSTENUTO, value),
                noteRepeatHandler::toggleActive,
                () -> noteRepeatHandler.getNoteRepeatActive().get(),
                this::showLiveValueInfo);
        this.liveControls = new NoteLiveControlSurface(livePerformanceControls, liveEncoderControls,
                encoderTouchDisplayHandler, this::showLiveValueInfo, this::showLiveDetailInfo,
                oled::clearScreenDelayed,
                driver.knobModeEncoderResetControl());
        this.livePadPerformer = new NoteLivePadPerformer(
                new NoteLivePadPerformer.MidiOut() {
                    @Override
                    public void noteOn(final int midiNote, final int velocity) {
                        noteInput.sendRawMidiEvent(Midi.NOTE_ON, midiNote, velocity);
                    }

                    @Override
                    public void noteOff(final int midiNote) {
                        noteInput.sendRawMidiEvent(Midi.NOTE_OFF, midiNote, 0);
                    }

                    @Override
                    public void timbre(final int midiNote, final int value) {
                        noteInput.sendRawMidiEvent(Midi.POLY_AT, midiNote, value);
                    }
                },
                this::getLivePadMidiNotes,
                (padIndex, configuredVelocity, rawVelocity) -> resolveLivePadVelocity(padIndex, configuredVelocity, rawVelocity),
                this::resolveLivePadTimbre);
        noteInput.assignPolyphonicAftertouchToExpression(0, NoteInput.NoteExpression.TIMBRE_UP, 1);
        this.liveExpressionControls = new NoteLiveExpressionControls(new NoteLiveExpressionControls.MidiExpressionOut() {
            @Override
            public void channelAftertouch(final int value) {
                noteInput.sendRawMidiEvent(Midi.CHANNEL_AT, value, 0);
            }

            @Override
            public void modulation(final int value) {
                noteInput.sendRawMidiEvent(Midi.CC, MIDI_CC_MOD, value);
            }

            @Override
            public void timbre(final int value) {
                noteInput.sendRawMidiEvent(Midi.CC, MIDI_CC_TIMBRE, value);
            }

            @Override
            public void breath(final int value) {
                noteInput.sendRawMidiEvent(Midi.CC, MIDI_CC_BREATH, value);
            }

            @Override
            public void pitchBend(final int bend) {
                noteInput.sendRawMidiEvent(Midi.PITCH_BEND, bend & 0x7F, (bend >> 7) & 0x7F);
            }
        });
        this.notePlayController = new NotePlayController(liveControls, livePadPerformer);

        for (int i = 0; i < liveRemoteControlsPage.getParameterCount(); i++) {
            ParameterEncoderBinding.markInterested(liveRemoteControlsPage.getParameter(i));
        }

        for (int i = 0; i < noteTranslationTable.length; i++) {
            noteTranslationTable[i] = -1;
        }

        bindPadBankRowControls();
        bindButtons();
        bindEncoders();
        applyDefaultLayoutPreference();
        applyDefaultVelocitySensitivityPreference();
    }

    private void observeLiveDrumPads() {
        liveDrumPadBank.canScrollBackwards().markInterested();
        liveDrumPadBank.canScrollForwards().markInterested();
        liveDrumPadBank.scrollPosition().markInterested();
        liveDrumPadBank.scrollPosition().addValueObserver(position -> {
            drumMachineScrollPosition = position;
            if (isDrumMachineLiveMode()) {
                retuneLivePads(() -> { });
            }
        });
        drumMachineScrollPosition = liveDrumPadBank.scrollPosition().get();
        for (int index = 0; index < DrumMachinePadLayout.PAD_WINDOW_SIZE; index++) {
            final DrumPad pad = liveDrumPadBank.getItemAt(index);
            final int padIndex = index;
            drumMachinePadColors[padIndex] = null;
            drumMachinePadNames[padIndex] = "";
            pad.exists().markInterested();
            pad.exists().addValueObserver(exists -> drumMachinePadExists[padIndex] = exists);
            drumMachinePadExists[padIndex] = pad.exists().get();
            pad.name().markInterested();
            pad.name().addValueObserver(name -> drumMachinePadNames[padIndex] = name);
            drumMachinePadNames[padIndex] = pad.name().get();
            pad.color().markInterested();
            pad.color().addValueObserver((r, g, b) ->
                    drumMachinePadColors[padIndex] = explicitDrumMachinePadColorOrNull(pad));
            drumMachinePadColors[padIndex] = explicitDrumMachinePadColorOrNull(pad);
        }
    }

    private static RgbLigthState explicitDrumMachinePadColorOrNull(final DrumPad pad) {
        final Color color = pad.color().get();
        if (color == null || color.getAlpha255() == 0) {
            return null;
        }
        final RgbLigthState light = ColorLookup.getColor(color);
        return light == null || light.equals(RgbLigthState.OFF) ? null : light;
    }

    private void applyDefaultLayoutPreference() {
        inKey = false;
        if (pitchContext.getScaleIndex() < 1) {
            driver.setSharedScaleIndex(1);
        }
    }

    private void applyDefaultVelocitySensitivityPreference() {
        liveVelocity.setSensitivity(driver.getDefaultVelocitySensitivityPreference());
    }

    private void handleSelectedTrackPeakMeterChanged(final int value) {
        selectedTrackPeakMeter = value;
        selectedTrackPeakMax = Math.max(selectedTrackPeakMax, value);
    }

    private void handleSelectedTrackRmsMeterChanged(final int value) {
        selectedTrackRmsMeter = value;
        selectedTrackRmsMax = Math.max(selectedTrackRmsMax, value);
    }

    private void resetSelectedTrackMeterMax() {
        selectedTrackPeakMax = selectedTrackPeakMeter;
        selectedTrackRmsMax = selectedTrackRmsMeter;
    }

    public void notifyBlink(final int blinkTicks) {
        refreshLiveMetersIfVisible(blinkTicks);
    }

    public boolean showIdleInfoIfNeeded() {
        if (stepInputHelperActive) {
            resetLiveMeterDisplay();
            showStepInputDisplay();
            return true;
        }
        if (shouldShowLiveMeters()) {
            showLiveMeterDisplay();
            liveMeterDisplayActive = true;
            lastMeterDisplayBlink = Integer.MIN_VALUE;
            return true;
        }
        if (hasLivePitchGlissOffset()) {
            resetLiveMeterDisplay();
            showLiveTrackLegendIdle();
            return true;
        }
        resetLiveMeterDisplay();
        if (!shouldShowLiveTrackLegendIdle()) {
            return false;
        }
        showLiveTrackLegendIdle();
        return true;
    }

    private void refreshLiveMetersIfVisible(final int blinkTicks) {
        if (!shouldShowLiveMeters()) {
            resetLiveMeterDisplay();
            refreshLiveContextIdleIfVisible();
            return;
        }
        if (!liveMeterDisplayActive || blinkTicks - lastMeterDisplayBlink >= METER_REFRESH_TICKS) {
            showLiveMeterDisplay();
            liveMeterDisplayActive = true;
            liveContextDisplayActive = false;
            lastMeterDisplayBlink = blinkTicks;
        }
    }

    private boolean shouldShowLiveMeters() {
        return shouldShowLiveMeters(active, drumPadsOnly, isDrumMachineLiveMode(),
                liveControls.currentEncoderMode(),
                driver.shouldShowMeterIdleDisplay()
                        && !oled.hasPendingTransientMessage()
                        && !hasLivePitchGlissOffset());
    }

    static boolean shouldShowLiveMeters(final boolean active,
                                        final boolean drumPadsOnly,
                                        final boolean drumMachineLiveMode,
                                        final EncoderMode encoderMode,
                                        final boolean meterIdleAllowed) {
        return active && !drumPadsOnly && !drumMachineLiveMode && meterIdleAllowed
                && encoderMode == EncoderMode.MIXER;
    }

    private void bindPadBankRowControls() {
        PadBankRowControlBindings.velocitySensitivePads(driver, this, new PadBankRowControlBindings.Host() {
            @Override
            public void handlePadPress(final int padIndex, final boolean pressed) {
                LivePadSurfaceLayer.this.handlePadPress(padIndex, pressed, 0);
            }

            @Override
            public void handlePadPress(final int padIndex, final boolean pressed, final int velocity) {
                LivePadSurfaceLayer.this.handlePadPress(padIndex, pressed, velocity);
            }

            @Override
            public RgbLigthState padLight(final int padIndex) {
                return LivePadSurfaceLayer.this.getPadLight(padIndex);
            }

            @Override
            public void handleBankButton(final boolean pressed, final int amount) {
                LivePadSurfaceLayer.this.handleBankButton(pressed, amount);
            }

            @Override
            public BiColorLightState bankLightState() {
                return LivePadSurfaceLayer.this.getBankLightState();
            }

            @Override
            public void handleRowButton(final int index, final boolean pressed) {
                LivePadSurfaceLayer.this.handleMuteButton(index, pressed);
            }

            @Override
            public BiColorLightState rowLightState(final int index) {
                return LivePadSurfaceLayer.this.muteLightState(index);
            }
        }, new PadBankRowControlBindings.ExtraButtonBinding(NoteAssign.STEP_SEQ,
                this::handleStepSeqPressed, this::getStepSeqLightState)).bind();
    }

    private void bindButtons() {
        final BiColorButton knobModeButton = driver.getButton(NoteAssign.KNOB_MODE);
        knobModeButton.bindPressed(liveModeControlLayer, this::handleLiveModeAdvance, this::getLiveModeLightState);
    }

    private void handleMuteButton(final int index, final boolean pressed) {
        switch (index) {
            case 0 -> handleMute1Button(pressed);
            case 1 -> handleMute2Button(pressed);
            case 2 -> handleMute3Button(pressed);
            case 3 -> handleMute4Button(pressed);
            default -> throw new IllegalArgumentException("Unsupported mute button index: " + index);
        }
    }

    private BiColorLightState muteLightState(final int index) {
        return switch (index) {
            case 0 -> getMute1LightState();
            case 1 -> getMute2LightState();
            case 2 -> getMute3LightState();
            case 3 -> getMute4LightState();
            default -> throw new IllegalArgumentException("Unsupported mute button index: " + index);
        };
    }

    private void bindEncoders() {
        final TouchEncoder[] encoders = driver.getEncoders();
        bindLiveChannelEncoders(encoders);
        bindLiveMixerEncoders(encoders);
        bindLiveExpressionEncoders(encoders);
        bindLiveRemoteEncoders(encoders);

        final TouchEncoder mainEncoder = driver.getMainEncoder();
        mainEncoder.bindEncoder(this, this::handleMainEncoder);
        mainEncoder.bindTouched(this, this::handleMainEncoderPress);
    }

    private void bindLiveChannelEncoders(final TouchEncoder[] encoders) {
        encoders[0].bindContinuousEncoder(liveChannelLayer, driver::isGlobalShiftHeld,
                com.oikoaudio.fire.control.ContinuousEncoderScaler.Profile.SOFT, inc -> {
                    if (inc == 0) {
                        return;
                    }
                    if (isDrumMachineLiveMode()) {
                        handleDrumMachineLayoutEncoder(0, inc);
                        return;
                    }
                    adjustLiveModulation(inc);
                });
        encoders[0].bindTouched(liveChannelLayer, touched -> liveControls.handleResettableTouch(0, touched,
                !isDrumMachineLiveMode(), "No reset",
                () -> {
                    if (isDrumMachineLiveMode()) {
                        showDrumMachineLayoutInfo();
                    } else {
                        oled.valueInfo("Mod", Integer.toString(liveExpressionControls.modulation()));
                    }
                },
                () -> {
                    if (!isDrumMachineLiveMode()) {
                        resetLiveModulation();
                    }
                }));

        encoders[1].bindContinuousEncoder(liveChannelLayer, driver::isGlobalShiftHeld,
                com.oikoaudio.fire.control.ContinuousEncoderScaler.Profile.SOFT, inc -> {
                    if (inc == 0) {
                        return;
                    }
                    if (isDrumMachineLiveMode()) {
                        handleLiveVelocityEncoder(1, inc);
                        return;
                    }
                    cancelLivePitchBendReturn();
                    adjustLivePitchBend(inc);
                });
        encoders[1].bindTouched(liveChannelLayer, touched -> {
            if (isDrumMachineLiveMode()) {
                liveControls.handleResettableTouch(1, touched,
                        this::showLiveVelocityInfo,
                        liveVelocityEncoder::reset);
                return;
            }
            handleLivePitchBendTouch(touched);
        });

        encoders[2].bindEncoder(liveChannelLayer, this::handleLivePitchGlissEncoder);
        encoders[2].bindTouched(liveChannelLayer, touched -> {
            if (touched) {
                resetLivePitchOffsetEncoder();
            }
            liveControls.handleResettableTouch(2, touched,
                    !isDrumMachineLiveMode(), "No reset",
                    () -> {
                        if (isDrumMachineLiveMode()) {
                            oled.valueInfo("Drum Pads", "--");
                            return;
                        }
                        if (driver.isGlobalAltHeld()) {
                            showLivePitchGlissModeInfo();
                        } else {
                            showLivePitchGlissInfo();
                        }
                    },
                    () -> {
                        if (!isDrumMachineLiveMode()) {
                            resetLivePitchGlissOffset();
                        }
                    });
        });

        encoders[3].bindContinuousEncoder(liveChannelLayer, driver::isGlobalShiftHeld,
                com.oikoaudio.fire.control.ContinuousEncoderScaler.Profile.SOFT, inc -> {
                    if (inc == 0 || isDrumMachineLiveMode()) {
                        return;
                    }
                    adjustLiveTimbre(inc);
                });
        encoders[3].bindTouched(liveChannelLayer, touched -> liveControls.handleResettableTouch(3, touched,
                !isDrumMachineLiveMode(), "No reset",
                () -> {
                    if (isDrumMachineLiveMode()) {
                        oled.valueInfo("Drum Pads", "--");
                        return;
                    }
                    oled.valueInfo("TimbreCC", Integer.toString(liveExpressionControls.timbre()));
                },
                this::resetLiveTimbre));
    }

    private void bindLiveExpressionEncoders(final TouchEncoder[] encoders) {
        encoders[0].bindEncoder(liveUser1Layer, inc -> handleLiveVelocityEncoder(0, inc));
        encoders[0].bindTouched(liveUser1Layer, touched -> liveControls.handleResettableTouch(0, touched,
                this::showLiveVelocityInfo,
                liveVelocityEncoder::reset));
        bindResettableLiveMidiEncoder(encoders[1], liveUser1Layer, 1, "Aftertouch",
                this::adjustLivePressure, () -> Integer.toString(liveExpressionControls.pressure()), this::resetLivePressure);
        bindResettableLiveMidiEncoder(encoders[2], liveUser1Layer, 2, "Breath",
                this::adjustLiveBreath, () -> Integer.toString(liveExpressionControls.breath()), this::resetLiveBreath);
        bindResettableLiveMidiEncoder(encoders[3], liveUser1Layer, 3, "Pitch Expr",
                this::adjustLivePitchExpression, this::formatLivePitchExpressionDisplay, this::resetLivePitchExpression);
    }

    private void bindLiveRemoteEncoders(final TouchEncoder[] encoders) {
        for (int i = 0; i < encoders.length; i++) {
            final int index = i;
            final Parameter parameter = liveRemoteControlsPage.getParameter(index);
            ParameterEncoderBinding.bind(encoders[i], liveUser2Layer, index, parameter,
                    "Remote " + (index + 1), driver::isGlobalShiftHeld,
                    ParameterEncoderBinding.ResetPolicy.PARAMETER_DEFAULT, driver.knobModeEncoderResetControl(),
                    oled::valueInfo, oled::clearScreenDelayed);
        }
    }

    private void bindLiveMixerEncoders(final TouchEncoder[] encoders) {
        final Track track = cursorTrack;
        final List<com.bitwig.extension.controller.api.Parameter> params = new ArrayList<>();
        params.add(track.volume());
        params.add(track.pan());
        params.add(track.sendBank().getItemAt(0));
        params.add(track.sendBank().getItemAt(1));
        for (final com.bitwig.extension.controller.api.Parameter parameter : params) {
            ParameterEncoderBinding.markInterested(parameter);
        }
        for (int i = 0; i < encoders.length; i++) {
            final int index = i;
            final com.bitwig.extension.controller.api.Parameter parameter = params.get(i);
            final EncoderTurnBehavior turnBehavior = EncoderTurnBehavior.acceleratedValue();
            final EncoderValueProfile profile = mixerEncoderValueProfile(index);
            final String fallbackLabel = switch (i) {
                case 0 -> "Volume";
                case 1 -> "Pan";
                case 2 -> "Send 1";
                default -> "Send 2";
            };
            encoders[i].bindRelativeMagnitudeEncoder(liveMixerLayer, rawUnits -> {
                final boolean fine = driver.isGlobalShiftHeld();
                final int inc = RelativeEncoderMagnitude.toStandardTurnStep(rawUnits, fine);
                if (inc == 0) {
                    return;
                }
                if (isHarmonicLiveMode()) {
                    handleHarmonicMixerEncoder(index, inc);
                    return;
                }
                final int effective = turnBehavior.apply(inc, fine);
                if (effective == 0) {
                    return;
                }
                ParameterEncoderBinding.adjustParameter(parameter, fine, effective, profile);
                ParameterEncoderBinding.showValueWithBar(parameter, fallbackLabel, this::showLiveMixerValue, index == 1);
            });
            encoders[i].bindTouched(liveMixerLayer, touched -> {
                if (touched) {
                    if (isHarmonicLiveMode()) {
                        if (handleHarmonicMixerReset(index)) {
                            return;
                        }
                        showHarmonicMixerInfo(index);
                    } else {
                        handleLiveMixerParameterTouch(index, parameter, fallbackLabel, true);
                    }
                } else {
                    handleLiveMixerParameterTouch(index, parameter, fallbackLabel, false);
                }
            });
        }
    }

    private void handleLiveMixerParameterTouch(final int encoderIndex, final Parameter parameter,
                                               final String fallbackLabel, final boolean touched) {
        if (touched) {
            final ParameterEncoderBinding.ResetPolicy resetPolicy = mixerResetPolicy(encoderIndex);
            if (driver.handleKnobModeEncoderReset(true,
                    ParameterEncoderBinding.isMapped(parameter)
                            && resetPolicy != ParameterEncoderBinding.ResetPolicy.NONE,
                    fallbackLabel, ParameterEncoderBinding.isMapped(parameter) ? "No reset" : "Unmapped",
                    () -> resetPolicy.reset(parameter),
                    () -> ParameterEncoderBinding.showValueWithBar(parameter, fallbackLabel,
                            this::showLiveMixerValue, encoderIndex == 1))) {
                return;
            }
            ParameterEncoderBinding.showValueWithBar(parameter, fallbackLabel, this::showLiveMixerValue,
                    encoderIndex == 1);
            return;
        }
        oled.clearScreenDelayed();
    }

    private void showLiveMixerValue(final String title, final String value, final double normalizedValue,
                                    final boolean biPolar) {
        selectedTrackMeterView.showValueInfo(title, value, normalizedValue, biPolar);
    }

    private ParameterEncoderBinding.ResetPolicy mixerResetPolicy(final int index) {
        return ParameterEncoderBinding.ResetPolicy.PARAMETER_DEFAULT;
    }

    private EncoderValueProfile mixerEncoderValueProfile(final int index) {
        return index == 1 ? EncoderValueProfile.PAN : EncoderValueProfile.LARGE_RANGE;
    }

    private void showLiveMeterDisplay() {
        showSelectedTrackMeterDisplay();
    }

    private void showSelectedTrackMeterDisplay() {
        selectedTrackMeterView.show(selectedTrackPeakMax, selectedTrackRmsMax,
                selectedTrackPeakMeter, selectedTrackRmsMeter,
                liveEncoderModeLegend(liveControls.currentEncoderMode()));
    }

    private void resetSelectedTrackMeterText() {
        selectedTrackMeterView.reset();
    }

    private void resetLiveMeterDisplay() {
        liveMeterDisplayActive = false;
        resetSelectedTrackMeterText();
    }

    private void resetLiveIdleDisplay() {
        resetLiveMeterDisplay();
        liveContextDisplayActive = false;
        liveContextDisplayRevision = Long.MIN_VALUE;
    }

    private boolean shouldShowLiveTrackLegendIdle() {
        return active && !drumPadsOnly;
    }

    private void showLiveTrackLegendIdle() {
        applyLiveFooterLegend();
        oled.clearScreen();
        oled.valueInfoPersistentNoClear(liveContextTitle(), normalizedSelectedTrackName());
        liveContextDisplayActive = true;
        liveContextDisplayRevision = oled.layoutRevision();
    }

    private String liveContextTitle() {
        if (!hasLivePitchGlissOffset()) {
            return currentNoteSubModeLabel();
        }
        return "Gliss " + compactLivePitchOffsetDisplay();
    }

    private void refreshLiveContextIdleIfVisible() {
        if (stepInputHelperActive) {
            showStepInputDisplay();
            return;
        }
        if (!shouldRefreshLiveContextIdle(isLiveContextDisplayCurrent(), oled.hasPendingTransientMessage(),
                shouldShowLiveTrackLegendIdle())) {
            return;
        }
        showLiveTrackLegendIdle();
    }

    private boolean isLiveContextDisplayCurrent() {
        return liveContextDisplayActive && liveContextDisplayRevision == oled.layoutRevision();
    }

    static boolean shouldRefreshLiveContextIdle(final boolean contextDisplayActive,
                                                final boolean pendingTransientMessage,
                                                final boolean trackLegendIdleAllowed) {
        return !contextDisplayActive && !pendingTransientMessage && trackLegendIdleAllowed;
    }

    private String normalizedSelectedTrackName() {
        final String trackName = cursorTrack.name().get();
        return trackName == null || trackName.isBlank() ? "Unnamed" : trackName;
    }

    private void showLiveValueInfo(final String title, final String value) {
        liveContextDisplayActive = false;
        applyLiveFooterLegend();
        oled.valueInfo(title, value);
    }

    private void showLiveDetailInfo(final String title, final String detail) {
        liveContextDisplayActive = false;
        applyLiveFooterLegend();
        oled.detailInfo(title, detail);
    }

    private void applyLiveFooterLegend() {
        oled.setFooterLegend(liveEncoderModeLegend(liveControls.currentEncoderMode()));
    }

    private void showStepInputDisplay() {
        liveContextDisplayActive = false;
        applyLiveFooterLegend();
        oled.clearScreen();
        oled.valueInfoPersistentNoClear("Step Input", stepInputStepLabel());
    }

    private String stepInputStepLabel() {
        final int displayStep = stepInputStepIndex + 1;
        final int totalSteps = estimatedStepInputTotalSteps(stepInputClipLengthBeats, STEP_INPUT_DISPLAY_STEP_SIZE_BEATS);
        return totalSteps > 0
                ? "Step %d/%d".formatted(displayStep, totalSteps)
                : "Step %d".formatted(displayStep);
    }

    static int estimatedStepInputTotalSteps(final double clipLengthBeats, final double stepSizeBeats) {
        if (clipLengthBeats <= 0.0 || stepSizeBeats <= 0.0) {
            return -1;
        }
        return Math.max(1, (int) Math.round(clipLengthBeats / stepSizeBeats));
    }

    private void handleHarmonicMixerEncoder(final int encoderIndex, final int inc) {
        switch (encoderIndex) {
            case 0 -> adjustHarmonicNoteCount(inc);
            case 1 -> adjustHarmonicOctaveSpan(inc);
            case 2 -> adjustHarmonicBassColumns(inc);
            case 3 -> adjustLivePitchOffset(inc);
            default -> { }
        }
    }

    private void showHarmonicMixerInfo(final int encoderIndex) {
        switch (encoderIndex) {
            case 0 -> oled.valueInfo("Notes", harmonicNoteCountDisplay());
            case 1 -> oled.valueInfo("Octaves", Integer.toString(harmonicOctaveSpan));
            case 2 -> oled.valueInfo("Bass Grid", harmonicBassColumns ? "On" : "Off");
            case 3 -> showLivePitchGlissInfo();
            default -> oled.clearScreenDelayed();
        }
    }

    private boolean handleHarmonicMixerReset(final int encoderIndex) {
        return switch (encoderIndex) {
            case 0 -> driver.handleKnobModeEncoderReset(true, true, "Notes", "No reset",
                    () -> retuneLivePads(() -> harmonicNoteCountIndex = 2),
                    () -> oled.valueInfo("Notes", harmonicNoteCountDisplay()));
            case 1 -> driver.handleKnobModeEncoderReset(true, true, "Octaves", "No reset",
                    () -> retuneLivePads(() -> harmonicOctaveSpan = 1),
                    () -> oled.valueInfo("Octaves", Integer.toString(harmonicOctaveSpan)));
            case 2 -> driver.handleKnobModeEncoderReset(true, true, "Bass Grid", "No reset",
                    () -> retuneLivePads(() -> harmonicBassColumns = true),
                    () -> oled.valueInfo("Bass Grid", harmonicBassColumns ? "On" : "Off"));
            case 3 -> driver.handleKnobModeEncoderReset(true, true, "Pitch Gliss", "No reset",
                    this::resetLivePitchGlissOffset,
                    this::showLivePitchGlissInfo);
            default -> false;
        };
    }

    private void bindResettableLiveMidiEncoder(final TouchEncoder encoder, final Layer layer, final int encoderIndex,
                                               final String label, final java.util.function.IntConsumer adjuster,
                                               final java.util.function.Supplier<String> valueSupplier,
                                               final Runnable resetAction) {
        bindResettableLiveMidiEncoder(encoder, layer, encoderIndex, label,
                com.oikoaudio.fire.control.ContinuousEncoderScaler.Profile.SOFT,
                adjuster, valueSupplier, resetAction);
    }

    private void bindResettableLiveMidiEncoder(final TouchEncoder encoder, final Layer layer, final int encoderIndex,
                                               final String label,
                                               final com.oikoaudio.fire.control.ContinuousEncoderScaler.Profile profile,
                                               final java.util.function.IntConsumer adjuster,
                                               final java.util.function.Supplier<String> valueSupplier,
                                               final Runnable resetAction) {
        encoder.bindContinuousEncoder(layer, driver::isGlobalShiftHeld,
                profile, inc -> {
            if (inc == 0) {
                return;
            }
            adjuster.accept(inc);
        });
        encoder.bindTouched(layer, touched -> liveControls.handleResettableTouch(encoderIndex, touched,
                () -> oled.valueInfo(label, valueSupplier.get()),
                resetAction));
    }

    private void bindLivePitchBendEncoder(final TouchEncoder encoder, final Layer layer, final int encoderIndex) {
        encoder.bindContinuousEncoder(layer, driver::isGlobalShiftHeld,
                com.oikoaudio.fire.control.ContinuousEncoderScaler.Profile.SOFT, inc -> {
                    if (inc == 0) {
                        return;
                    }
                    cancelLivePitchBendReturn();
                    adjustLivePitchBend(inc);
                });
        encoder.bindTouched(layer, this::handleLivePitchBendTouch);
    }

    private void handleLiveVelocityEncoder(final int inc) {
        handleLiveVelocityEncoder(1, inc);
    }

    private void handleLiveVelocityEncoder(final int encoderIndex, final int inc) {
        final int steps = liveVelocityEncoder.consume(inc);
        if (steps == 0) {
            return;
        }
        if (driver.isGlobalShiftHeld()) {
            if (!liveVelocity.adjustCenterVelocity(steps)) {
                return;
            }
            applyLiveVelocity();
            oled.paramInfo("Velocity Center", liveVelocity.centerVelocity(), "",
                    liveVelocity.minCenterVelocity(), liveVelocity.maxCenterVelocity());
            return;
        }
        if (!liveVelocity.adjustSensitivity(steps)) {
            return;
        }
        oled.paramInfo("Velocity Sens", liveVelocity.sensitivity(), "", 0, 100);
    }

    private void handleDrumMachineLayoutEncoder(final int encoderIndex, final int inc) {
        final int steps = liveScaleEncoder.consume(inc);
        if (steps == 0) {
            return;
        }
        adjustDrumMachineLayout(steps);
    }

    private void adjustLivePressure(final int inc) {
        if (liveExpressionControls.adjustPressure(inc)) {
            oled.paramInfo("Aftertouch", liveExpressionControls.pressure(), "", MIN_MIDI_VALUE, MAX_MIDI_VALUE);
        }
    }

    private void adjustLiveTimbre(final int inc) {
        if (liveExpressionControls.adjustTimbre(inc)) {
            oled.paramInfo("TimbreCC", liveExpressionControls.timbre(), "", MIN_MIDI_VALUE, MAX_MIDI_VALUE);
        }
    }

    private void adjustLiveModulation(final int inc) {
        if (liveExpressionControls.adjustModulation(inc)) {
            oled.paramInfo("Mod", liveExpressionControls.modulation(), "", MIN_MIDI_VALUE, MAX_MIDI_VALUE);
        }
    }

    private void adjustLiveBreath(final int inc) {
        if (liveExpressionControls.adjustBreath(inc)) {
            oled.paramInfo("Breath", liveExpressionControls.breath(), "", MIN_MIDI_VALUE, MAX_MIDI_VALUE);
        }
    }

    private void adjustLivePitchBend(final int inc) {
        final int next = Math.max(MIN_MIDI_VALUE, Math.min(MAX_MIDI_VALUE, livePitchBend + inc));
        if (next == livePitchBend) {
            return;
        }
        livePitchBend = next;
        liveExpressionControls.setTransientPitchBendValue(livePitchBend);
        showLivePitchBendInfo();
        armLivePitchBendInactivityReturn();
    }

    private void adjustLivePitchExpression(final int inc) {
        if (liveExpressionControls.adjustPitchExpression(inc)) {
            oled.valueInfo("Pitch Expr", formatLivePitchExpressionDisplay());
        }
    }

    private void handleLivePitchGlissEncoder(final int inc) {
        if (isDrumMachineLiveMode()) {
            return;
        }
        if (driver.isGlobalAltHeld()) {
            toggleLivePitchGlissMode(inc);
            return;
        }
        adjustLivePitchOffset(inc);
    }

    private void toggleLivePitchGlissMode(final int inc) {
        if (inc == 0) {
            return;
        }
        final LivePitchGlissMode next = inc < 0 ? LivePitchGlissMode.FIFTH_OCTAVE : LivePitchGlissMode.SCALE_DEGREE;
        if (next == livePitchGlissMode) {
            showLivePitchGlissModeInfo();
            return;
        }
        livePitchGlissMode = next;
        showLivePitchGlissModeInfo();
    }

    private void adjustLivePitchOffset(final int inc) {
        final int steps = consumeLivePitchOffsetEncoder(inc);
        if (steps == 0) {
            return;
        }
        if (livePitchGlissMode == LivePitchGlissMode.FIFTH_OCTAVE) {
            final int nextIndex = Math.max(0, Math.min(LIVE_PITCH_OFFSETS.length - 1, livePitchOffsetIndex + steps));
            if (nextIndex == livePitchOffsetIndex) {
                return;
            }
            retuneLivePads(() -> livePitchOffsetIndex = nextIndex);
            showLivePitchGlissInfo();
            return;
        }
        final int nextOffset = Math.max(MIN_SCALE_DEGREE_GLISS,
                Math.min(MAX_SCALE_DEGREE_GLISS, liveScaleDegreeGlissOffset + steps));
        if (nextOffset == liveScaleDegreeGlissOffset) {
            return;
        }
        retuneLivePads(() -> liveScaleDegreeGlissOffset = nextOffset);
        showLivePitchGlissInfo();
    }

    private void showLivePitchGlissInfo() {
        oled.valueInfo("Pitch Gliss", formatLivePitchOffsetDisplay());
    }

    private void showLivePitchGlissModeInfo() {
        oled.valueInfo("Gliss Mode", livePitchGlissMode.displayName());
    }

    private void showLivePitchBendInfo() {
        oled.valueInfo("Pitch Bend", formatSignedValue(livePitchBend - DEFAULT_LIVE_PITCH_BEND));
    }

    private int consumeLivePitchOffsetEncoder(final int inc) {
        if (inc == 0) {
            return 0;
        }
        if (livePitchOffsetFirstStepPending) {
            livePitchOffsetFirstStepPending = false;
            return inc > 0 ? 1 : -1;
        }
        livePitchOffsetEncoderCarry += inc;
        final int steps = livePitchOffsetEncoderCarry / LIVE_PITCH_OFFSET_ENCODER_THRESHOLD;
        livePitchOffsetEncoderCarry %= LIVE_PITCH_OFFSET_ENCODER_THRESHOLD;
        return steps;
    }

    private void resetLivePitchOffsetEncoder() {
        livePitchOffsetEncoderCarry = 0;
        livePitchOffsetFirstStepPending = true;
    }

    private void resetLivePitchGlissOffset() {
        resetLivePitchOffsetEncoder();
        retuneLivePads(() -> {
            livePitchOffsetIndex = DEFAULT_LIVE_PITCH_OFFSET_INDEX;
            liveScaleDegreeGlissOffset = 0;
        });
    }

    private void adjustHarmonicNoteCount(final int inc) {
        final int steps = liveVelocityEncoder.consume(inc);
        if (steps == 0) {
            return;
        }
        final int next = Math.max(0, Math.min(HarmonicLatticeLayout.NOTE_COUNTS.length - 1, harmonicNoteCountIndex + steps));
        if (next == harmonicNoteCountIndex) {
            return;
        }
        retuneLivePads(() -> harmonicNoteCountIndex = next);
        oled.valueInfo("Notes", harmonicNoteCountDisplay());
    }

    private void adjustHarmonicOctaveSpan(final int inc) {
        final int steps = liveOctaveEncoder.consume(inc);
        if (steps == 0) {
            return;
        }
        final int next = Math.max(1, Math.min(3, harmonicOctaveSpan + steps));
        if (next == harmonicOctaveSpan) {
            return;
        }
        retuneLivePads(() -> harmonicOctaveSpan = next);
        oled.valueInfo("Octaves", Integer.toString(harmonicOctaveSpan));
    }

    private void adjustHarmonicBassColumns(final int inc) {
        final int steps = liveLayoutEncoder.consume(inc);
        if (steps == 0) {
            return;
        }
        final boolean next = steps > 0;
        if (next == harmonicBassColumns) {
            return;
        }
        retuneLivePads(() -> harmonicBassColumns = next);
        oled.valueInfo("Bass Grid", harmonicBassColumns ? "On" : "Off");
    }

    private String harmonicNoteCountDisplay() {
        return Integer.toString(HarmonicLatticeLayout.NOTE_COUNTS[harmonicNoteCountIndex]);
    }

    private int getHarmonicGlissStepOffset() {
        return livePitchGlissMode == LivePitchGlissMode.FIFTH_OCTAVE
                ? livePitchOffsetIndex - DEFAULT_LIVE_PITCH_OFFSET_INDEX
                : liveScaleDegreeGlissOffset;
    }

    static int displayPitchGlissValue(final boolean fifthOctaveMode,
                                      final int livePitchOffset,
                                      final int scaleDegreeGlissOffset) {
        if (!fifthOctaveMode) {
            return scaleDegreeGlissOffset;
        }
        return livePitchOffset;
    }

    private String formatLivePitchExpressionDisplay() {
        return formatSignedValue(liveExpressionControls.pitchExpression() - DEFAULT_LIVE_PITCH_EXPRESSION);
    }

    private String formatLivePitchOffsetDisplay() {
        final int value = displayPitchGlissValue(livePitchGlissMode == LivePitchGlissMode.FIFTH_OCTAVE,
                getLivePitchOffset(), liveScaleDegreeGlissOffset);
        return "%s %s".formatted(livePitchGlissMode.displayName(), formatSignedValue(value));
    }

    private String compactLivePitchOffsetDisplay() {
        final int value = displayPitchGlissValue(livePitchGlissMode == LivePitchGlissMode.FIFTH_OCTAVE,
                getLivePitchOffset(), liveScaleDegreeGlissOffset);
        return formatSignedValue(value);
    }

    private boolean hasLivePitchGlissOffset() {
        return displayPitchGlissValue(livePitchGlissMode == LivePitchGlissMode.FIFTH_OCTAVE,
                getLivePitchOffset(), liveScaleDegreeGlissOffset) != 0;
    }

    private String liveEncoderModeInfo(final EncoderMode mode) {
        if (isDrumMachineLiveMode() && mode == EncoderMode.CHANNEL) {
            return "1: Layout\n2: Velocity\n3: --\n4: --";
        }
        if (isHarmonicLiveMode() && mode == EncoderMode.MIXER) {
            return "1: Notes\n2: Octaves\n3: Bass Grid\n4: Pitch Gliss";
        }
        if (mode == EncoderMode.USER_2) {
            return EncoderFooterLegend.remoteModeInfo("Device Remotes", "D", 1,
                    liveRemoteParameterNames(0));
        }
        return NoteLiveEncoderModeControls.modeInfo(mode);
    }

    private String liveEncoderModeLegend(final EncoderMode mode) {
        if (isDrumMachineLiveMode() && mode == EncoderMode.CHANNEL) {
            return EncoderFooterLegend.of("Lay", "Velo", "--", "--");
        }
        if (isHarmonicLiveMode() && mode == EncoderMode.MIXER) {
            return EncoderFooterLegend.of("Note", "Oct", "Bass", "Glis");
        }
        if (mode == EncoderMode.USER_2) {
            return EncoderFooterLegend.remoteControls("D", 1, liveRemoteParameterNames(0));
        }
        return NoteLiveEncoderModeControls.modeLegend(mode);
    }

    private String[] liveRemoteParameterNames(final int firstParameterIndex) {
        final String[] names = new String[4];
        for (int slot = 0; slot < names.length; slot++) {
            final int parameterIndex = firstParameterIndex + slot;
            if (parameterIndex < liveRemoteControlsPage.getParameterCount()) {
                names[slot] = liveRemoteControlsPage.getParameter(parameterIndex).name().get();
            }
        }
        return names;
    }

    private void resetLivePressure() {
        liveExpressionControls.resetPressure();
    }

    private void resetLiveModulation() {
        liveExpressionControls.resetModulation();
    }

    private void resetLiveTimbre() {
        liveExpressionControls.resetTimbre();
    }

    private void resetLiveBreath() {
        liveExpressionControls.resetBreath();
    }

    private void resetLivePitchExpression() {
        liveExpressionControls.resetPitchExpression();
    }

    private void handleLivePitchBendTouch(final boolean touched) {
        if (isDrumMachineLiveMode()) {
            if (touched) {
                showDrumMachineLayoutInfo();
            } else {
                oled.clearScreenDelayed();
            }
            return;
        }
        livePitchBendTouched = touched;
        cancelLivePitchBendReturn();
        if (touched) {
            if (driver.handleKnobModeEncoderReset(true, !isDrumMachineLiveMode(), "Pitch Bend", "No reset",
                    () -> {
                        livePitchBend = DEFAULT_LIVE_PITCH_BEND;
                        liveExpressionControls.setTransientPitchBendValue(livePitchBend);
                    },
                    this::showLivePitchBendInfo)) {
                return;
            }
            showLivePitchBendInfo();
            return;
        }
        scheduleLivePitchBendReturn();
    }

    private void scheduleLivePitchBendReturn() {
        if (livePitchBend == DEFAULT_LIVE_PITCH_BEND) {
            oled.clearScreenDelayed();
            return;
        }
        final int generation = ++livePitchBendReturnGeneration;
        driver.getHost().scheduleTask(() -> continueLivePitchBendReturn(generation), LIVE_PITCH_BEND_RETURN_DELAY_MS);
    }

    private void continueLivePitchBendReturn(final int generation) {
        if (generation != livePitchBendReturnGeneration || livePitchBendTouched) {
            return;
        }
        if (livePitchBend < DEFAULT_LIVE_PITCH_BEND) {
            livePitchBend = Math.min(DEFAULT_LIVE_PITCH_BEND, livePitchBend + LIVE_PITCH_BEND_RETURN_STEP);
        } else if (livePitchBend > DEFAULT_LIVE_PITCH_BEND) {
            livePitchBend = Math.max(DEFAULT_LIVE_PITCH_BEND, livePitchBend - LIVE_PITCH_BEND_RETURN_STEP);
        }
        liveExpressionControls.setTransientPitchBendValue(livePitchBend);
        showLivePitchBendInfo();
        if (livePitchBend == DEFAULT_LIVE_PITCH_BEND) {
            oled.clearScreenDelayed();
            return;
        }
        driver.getHost().scheduleTask(() -> continueLivePitchBendReturn(generation), LIVE_PITCH_BEND_RETURN_DELAY_MS);
    }

    private void cancelLivePitchBendReturn() {
        livePitchBendReturnGeneration++;
    }

    private void armLivePitchBendInactivityReturn() {
        final int generation = ++livePitchBendInactivityGeneration;
        driver.getHost().scheduleTask(() -> {
            if (generation != livePitchBendInactivityGeneration || livePitchBendTouched
                    || livePitchBend == DEFAULT_LIVE_PITCH_BEND) {
                return;
            }
            scheduleLivePitchBendReturn();
        }, LIVE_PITCH_BEND_INACTIVITY_RETURN_MS);
    }

    private int getLivePitchOffset() {
        return LIVE_PITCH_OFFSETS[livePitchOffsetIndex];
    }

    private void handleMute1Button(final boolean pressed) {
        notePlayController.handleMute1(pressed);
    }

    private void handleMute2Button(final boolean pressed) {
        notePlayController.handleMute2(pressed);
    }

    private void handleMute3Button(final boolean pressed) {
        notePlayController.handleMute3(pressed);
    }

    private void handleMute4Button(final boolean pressed) {
    }

    private BiColorLightState getMute1LightState() {
        return notePlayController.mute1LightState();
    }

    private BiColorLightState getMute2LightState() {
        return notePlayController.mute2LightState();
    }

    private BiColorLightState getMute3LightState() {
        return notePlayController.mute3LightState();
    }

    private BiColorLightState getMute4LightState() {
        return BiColorLightState.OFF;
    }

    private void handleStepSeqPressed(final boolean pressed) {
        if (pressed && driver.isGlobalShiftHeld() && !driver.isGlobalAltHeld()) {
            toggleStepInputHelper();
            return;
        }
        if (pressed && !driver.isGlobalShiftHeld() && !driver.isGlobalAltHeld()) {
            driver.enterPlainStepPressTarget();
        }
    }

    private void toggleStepInputHelper() {
        if (stepInputHelperActive) {
            deactivateStepInputHelper(false);
            return;
        }
        activateStepInputHelper();
    }

    private void activateStepInputHelper() {
        if (isDrumMachineLiveMode()) {
            oled.valueInfo("Step Input", "Note/Harmonic");
            return;
        }
        final Application application = driver.getApplication();
        stepInputCursorClip.showInEditor();
        final int generation = ++stepInputActivationGeneration;
        oled.valueInfo("Step Input", "Opening");
        driver.getHost().scheduleTask(() -> focusStepInputTrack(application, generation), 100);
    }

    private void focusStepInputTrack(final Application application, final int generation) {
        if (generation != stepInputActivationGeneration || isDrumMachineLiveMode()) {
            return;
        }
        cursorTrack.selectInEditor();
        BitwigEditorToolActions.focusTrackHeader(application);
        driver.getHost().scheduleTask(() -> focusStepInputEditor(application, generation), 100);
    }

    private void focusStepInputEditor(final Application application, final int generation) {
        if (generation != stepInputActivationGeneration || isDrumMachineLiveMode()) {
            return;
        }
        if (!BitwigEditorToolActions.focusClipEditorPanel(application)) {
            driver.notifyPopup("Step Input", "Editor focus unavailable");
            BitwigEditorToolActions.logCandidateActions(application,
                    message -> driver.getHost().println("[Oikontrol] " + message));
        }
        driver.getHost().scheduleTask(() -> completeStepInputActivation(application, generation), 100);
    }

    private void completeStepInputActivation(final Application application, final int generation) {
        if (generation != stepInputActivationGeneration || isDrumMachineLiveMode()) {
            return;
        }
        if (!BitwigEditorToolActions.activateStepInputTool(application)) {
            oled.valueInfo("Step Input", "Unavailable");
            driver.notifyPopup("Step Input", "Tool unavailable");
            BitwigEditorToolActions.logCandidateActions(application,
                    message -> driver.getHost().println("[Oikontrol] " + message));
            return;
        }
        if (!BitwigEditorToolActions.moveTimeSelectionToFirstItem(application)) {
            driver.notifyPopup("Step Input", "Start action unavailable");
            BitwigEditorToolActions.logCandidateActions(application,
                    message -> driver.getHost().println("[Oikontrol] " + message));
        }
        stepInputHelperActive = true;
        stepInputStepIndex = 0;
        stepInputHeldPadCount = 0;
        stepInputPadGesturePending = false;
        resetLiveIdleDisplay();
        showStepInputDisplay();
        driver.notifyPopup("Step Input", "On");
    }

    private void deactivateStepInputHelper(final boolean force) {
        stepInputActivationGeneration++;
        final Application application = driver.getApplication();
        if (!force && !BitwigEditorToolActions.activatePointerTool(application)) {
            oled.valueInfo("Pointer", "Unavailable");
            driver.notifyPopup("Pointer", "Tool unavailable");
            BitwigEditorToolActions.logCandidateActions(application,
                    message -> driver.getHost().println("[Oikontrol] " + message));
            return;
        }
        stepInputHelperActive = false;
        stepInputHeldPadCount = 0;
        stepInputPadGesturePending = false;
        resetLiveIdleDisplay();
        showLiveTrackLegendIdle();
        driver.notifyPopup("Step Input", "Off");
    }

    private boolean handleStepInputBankButton(final boolean pressed, final int amount) {
        if (!stepInputHelperActive || !pressed || driver.isGlobalAltHeld()) {
            return false;
        }
        final Application application = driver.getApplication();
        if (application == null) {
            oled.valueInfo("Step Input", "Unavailable");
            return true;
        }
        if (amount > 0) {
            application.arrowKeyRight();
            advanceStepInputEstimate(1);
            return true;
        }
        application.arrowKeyLeft();
        advanceStepInputEstimate(-1);
        return true;
    }

    private void handleStepInputPadGesture(final boolean pressed) {
        if (!stepInputHelperActive || isDrumMachineLiveMode()) {
            return;
        }
        if (pressed) {
            if (stepInputHeldPadCount == 0) {
                stepInputPadGesturePending = true;
            }
            stepInputHeldPadCount++;
            return;
        }
        if (stepInputHeldPadCount > 0) {
            stepInputHeldPadCount--;
        }
        if (stepInputHeldPadCount == 0 && stepInputPadGesturePending) {
            stepInputPadGesturePending = false;
            advanceStepInputEstimate(1);
        }
    }

    private void advanceStepInputEstimate(final int amount) {
        stepInputStepIndex = Math.max(0, stepInputStepIndex + amount);
        showStepInputDisplay();
    }

    private void clearStepInputHelper() {
        if (!stepInputHelperActive) {
            return;
        }
        BitwigEditorToolActions.activatePointerTool(driver.getApplication());
        stepInputActivationGeneration++;
        stepInputHelperActive = false;
        stepInputHeldPadCount = 0;
        stepInputPadGesturePending = false;
    }

    private void handlePadPress(final int padIndex, final boolean pressed, final int velocity) {
        if (isDrumMachineLiveMode() && drumMachineLayout == DrumMachinePadLayout.Layout.BONGOS
                && handleBongoSurfaceGate(padIndex, pressed)) {
            return;
        }
        if (pressed && isDrumMachineLiveMode() && handleDrumMachinePadPress(padIndex)) {
            return;
        }
        notePlayController.handlePadPress(padIndex, pressed, velocity, liveVelocity.centerVelocity());
        handleStepInputPadGesture(pressed);
    }

    private boolean handleBongoSurfaceGate(final int padIndex, final boolean pressed) {
        final int zone = bongoZoneForPad(padIndex);
        if (zone < 0) {
            return false;
        }
        if (pressed) {
            final boolean zoneAlreadyHeld = heldBongoPadCounts[zone] > 0;
            if (!heldBongoPads[zone][padIndex]) {
                heldBongoPads[zone][padIndex] = true;
                heldBongoPadCounts[zone]++;
            }
            if (zoneAlreadyHeld) {
                return true;
            }
            return false;
        }
        if (heldBongoPads[zone][padIndex]) {
            heldBongoPads[zone][padIndex] = false;
            heldBongoPadCounts[zone] = Math.max(0, heldBongoPadCounts[zone] - 1);
        }
        return false;
    }

    private int bongoZoneForPad(final int padIndex) {
        final LiveNoteLayout layout = createLayout();
        if (!(layout instanceof DrumMachinePadLayout drumMachinePadLayout)
                || drumMachinePadLayout.selectorOffsetForPad(padIndex) >= 0) {
            return -1;
        }
        final int column = padIndex % NoteGridLayout.PAD_COLUMNS;
        if (column < 5 || column == 10) {
            return -1;
        }
        return column >= 11 ? 1 : 0;
    }

    private boolean handleDrumMachinePadPress(final int padIndex) {
        final LiveNoteLayout layout = createLayout();
        if (!(layout instanceof DrumMachinePadLayout drumMachinePadLayout)) {
            return false;
        }
        final int selectorOffset = drumMachinePadLayout.selectorOffsetForPad(padIndex);
        if (selectorOffset >= 0) {
            assignDrumMachineSelector(selectorOffset);
            showDrumMachinePadInfo(padIndex);
            return false;
        }
        showDrumMachinePadInfo(padIndex);
        return false;
    }

    private void assignDrumMachineSelector(final int selectorOffset) {
        if (drumMachineLayout == DrumMachinePadLayout.Layout.BONGOS) {
            final int heldZone = heldBongoPadCounts[1] > 0 ? 1 : heldBongoPadCounts[0] > 0 ? 0 : -1;
            if (heldZone == 1) {
                secondaryDrumPadOffset = selectorOffset;
                return;
            }
            if (heldZone == 0) {
                selectedDrumPadOffset = selectorOffset;
                return;
            }
        }
        selectedDrumPadOffset = selectorOffset;
        if (drumMachineLayout == DrumMachinePadLayout.Layout.BONGOS
                && secondaryDrumPadOffset == selectedDrumPadOffset) {
            secondaryDrumPadOffset = Math.min(DrumMachinePadLayout.PAD_WINDOW_SIZE - 1, selectorOffset + 1);
        }
    }

    private void showDrumMachinePadInfo(final int padIndex) {
        final LiveNoteLayout layout = createLayout();
        if (!(layout instanceof DrumMachinePadLayout drumMachinePadLayout)) {
            return;
        }
        final int bankIndex = drumMachinePadLayout.padBankIndexForPad(padIndex);
        if (bankIndex < 0 || bankIndex >= drumMachinePadNames.length) {
            return;
        }
        final int midiNote = drumMachineScrollPosition + bankIndex;
        final String name = drumMachinePadNames[bankIndex] == null || drumMachinePadNames[bankIndex].isBlank()
                ? "MIDI " + midiNote
                : drumMachinePadNames[bankIndex];
        oled.valueInfo("Drum Pad", name);
    }

    private void applyLiveEncoderStepSizes(final EncoderMode mode) {
        final TouchEncoder[] encoders = driver.getEncoders();
        switch (mode) {
            case CHANNEL -> {
                encoders[0].setStepSize(MixerEncoderProfile.STEP_SIZE);
                encoders[1].setStepSize(MixerEncoderProfile.STEP_SIZE);
                encoders[2].setStepSize(MixerEncoderProfile.STEP_SIZE);
                encoders[3].setStepSize(MixerEncoderProfile.STEP_SIZE);
            }
            case MIXER, USER_1, USER_2 -> {
                for (final TouchEncoder encoder : encoders) {
                    encoder.setStepSize(MixerEncoderProfile.STEP_SIZE);
                }
            }
        }
    }

    private void handleBankButton(final boolean pressed, final int amount) {
        if (handleStepInputBankButton(pressed, amount)) {
            return;
        }
        if (driver.handleGlobalUndoRedoBankButton(pressed, amount)) {
            return;
        }
        if (!pressed) {
            return;
        }
        if (isDrumMachineLiveMode()) {
            oled.valueInfo("Pad Window", "Use Pattern");
            return;
        }
        adjustOctave(amount);
    }

    private void activateDrumPadPatternButtons() {
        final PatternButtons patternButtons = driver.getPatternButtons();
        if (patternButtons == null) {
            return;
        }
        patternButtons.setUpCallback(pressed -> {
            if (!pressed) {
                return;
            }
            if (isDrumMachineLiveMode()) {
                scrollDrumMachineWindow(DRUM_MACHINE_SCROLL_COARSE_STEPS);
            } else {
                handleLivePatternButton(-1);
            }
        }, () -> isDrumMachineLiveMode()
                ? (canScrollDrumMachineWindow(DRUM_MACHINE_SCROLL_COARSE_STEPS)
                ? BiColorLightState.AMBER_HALF
                : BiColorLightState.OFF)
                : BiColorLightState.AMBER_HALF);
        patternButtons.setDownCallback(pressed -> {
            if (!pressed) {
                return;
            }
            if (isDrumMachineLiveMode()) {
                scrollDrumMachineWindow(-DRUM_MACHINE_SCROLL_COARSE_STEPS);
            } else {
                handleLivePatternButton(1);
            }
        }, () -> isDrumMachineLiveMode()
                ? (canScrollDrumMachineWindow(-DRUM_MACHINE_SCROLL_COARSE_STEPS)
                ? BiColorLightState.AMBER_HALF
                : BiColorLightState.OFF)
                : BiColorLightState.AMBER_HALF);
    }

    private void handleLivePatternButton(final int direction) {
        if (driver.isGlobalAltHeld()) {
            adjustTransposeSemitone(-direction);
            return;
        }
        adjustScale(direction);
    }

    private void clearDrumPadPatternButtons() {
        final PatternButtons patternButtons = driver.getPatternButtons();
        if (patternButtons == null) {
            return;
        }
        patternButtons.setUpCallback(pressed -> { }, () -> BiColorLightState.OFF);
        patternButtons.setDownCallback(pressed -> { }, () -> BiColorLightState.OFF);
    }

    private void handleLiveModeAdvance(final boolean pressed) {
        if (pressed) {
            return;
        }
        if (driver.consumeKnobModeGesture()) {
            oled.clearScreenDelayed();
            return;
        }
        notePlayController.handleModeAdvance(true, false);
    }

    private BiColorLightState getLiveModeLightState() {
        return notePlayController.modeLightState();
    }

    public AkaiFireOikontrolExtension.RemotePageTarget currentRemotePageTarget() {
        return liveControls.currentEncoderMode() == EncoderMode.USER_2
                ? new AkaiFireOikontrolExtension.RemotePageTarget(liveRemoteControlsPage, "Device")
                : null;
    }

    private void handlePitchContextButton(final boolean pressed, final int amount, final boolean root) {
        if (!pressed) {
            return;
        }
        if (root) {
            adjustTransposeSemitone(amount);
        } else {
            adjustOctave(amount);
        }
    }

    private void showTouchedState(final boolean pressed, final String label) {
        if (pressed) {
            showState(label);
        } else {
            oled.clearScreenDelayed();
        }
    }

    private BiColorLightState getBankLightState() {
        return BiColorLightState.HALF;
    }

    private BiColorLightState getPitchContextLightState(final int amount, final boolean root) {
        if (root) {
            return BiColorLightState.AMBER_HALF;
        }
        return amount < 0
                ? (getOctave() > MIN_OCTAVE ? BiColorLightState.AMBER_HALF : BiColorLightState.OFF)
                : (getOctave() < MAX_OCTAVE ? BiColorLightState.AMBER_HALF : BiColorLightState.OFF);
    }

    private BiColorLightState getStepSeqLightState() {
        return BiColorLightState.OFF;
    }

    private boolean isHarmonicLiveMode() {
        return liveNoteSubMode == LiveNoteSubMode.HARMONIC;
    }

    private boolean isDrumMachineLiveMode() {
        return liveNoteSubMode == LiveNoteSubMode.DRUM_PADS;
    }

    public String currentNoteSubModeLabel() {
        if (liveNoteSubMode == LiveNoteSubMode.DRUM_PADS) {
            return "Drum Pads";
        }
        return liveNoteSubMode.displayName();
    }

    public boolean isHarmonicNoteSubMode() {
        return liveNoteSubMode == LiveNoteSubMode.HARMONIC;
    }

    public void resetNoteSubMode() {
        if (drumPadsOnly || liveNoteSubMode == LiveNoteSubMode.MELODIC) {
            return;
        }
        applyLayoutChange(() -> liveNoteSubMode = LiveNoteSubMode.MELODIC);
    }

    public void cycleNoteSubMode() {
        if (drumPadsOnly) {
            showDrumMachineLayoutInfo();
            return;
        }
        final LiveNoteSubMode next = liveNoteSubMode == LiveNoteSubMode.MELODIC
                ? LiveNoteSubMode.HARMONIC
                : LiveNoteSubMode.MELODIC;
        applyLayoutChange(() -> {
            liveNoteSubMode = next;
        });
    }

    private void toggleLayout() {
        if (isHarmonicLiveMode()) {
            harmonicBassColumns = !harmonicBassColumns;
            retuneLivePads(() -> { });
            showState("Layout");
            return;
        }
        if (isDrumMachineLiveMode()) {
            showDrumMachineLayoutInfo();
            return;
        }
        applyLayoutChange(() -> {
            inKey = !inKey;
        });
        showState("Layout");
    }

    private void adjustLayout(final int amount) {
        if (amount == 0) {
            return;
        }
        if (isHarmonicLiveMode()) {
            if ((amount > 0 && !harmonicBassColumns) || (amount < 0 && harmonicBassColumns)) {
                toggleLayout();
            }
            return;
        }
        if (isDrumMachineLiveMode()) {
            adjustDrumMachineLayout(amount);
            return;
        }
        if (amount > 0 && !inKey) {
            toggleLayout();
            return;
        }
        if (amount < 0 && inKey) {
            toggleLayout();
        }
    }

    private void adjustDrumMachineLayout(final int amount) {
        if (amount == 0) {
            return;
        }
        final DrumMachinePadLayout.Layout[] layouts = DrumMachinePadLayout.Layout.values();
        final int nextIndex = Math.max(0, Math.min(layouts.length - 1, drumMachineLayout.ordinal() + amount));
        if (nextIndex == drumMachineLayout.ordinal()) {
            showDrumMachineLayoutInfo();
            return;
        }
        retuneLivePads(() -> drumMachineLayout = layouts[nextIndex]);
        showDrumMachineLayoutInfo();
    }

    public void toggleSurfaceVariant() {
        cycleNoteSubMode();
    }

    public void toggleLiveLayoutShortcut() {
        toggleLayout();
    }

    public BiColorLightState getModeButtonLightState() {
        return switch (liveNoteSubMode) {
            case MELODIC -> ModeButtonLights.MODE_1;
            case HARMONIC -> ModeButtonLights.MODE_2;
            case DRUM_PADS -> ModeButtonLights.MODE_3;
        };
    }

    private void adjustScale(final int amount) {
        if (amount == 0) {
            return;
        }
        final int minScale = liveScaleMinIndex();
        final int nextScale = pitchContext.getScaleIndex() + amount;
        if (nextScale < minScale || nextScale >= pitchContext.getScaleCount()) {
            return;
        }
        applyLayoutChange(() -> driver.setSharedScaleIndex(nextScale));
        showState("Scale");
    }

    public void adjustSharedScaleFromOverview(final int amount) {
        driver.adjustSharedScaleIndex(amount, liveScaleMinIndex());
    }

    private void adjustOctave(final int amount) {
        if (amount == 0) {
            return;
        }
        if (isDrumMachineLiveMode()) {
            scrollDrumMachineWindow(amount * DRUM_MACHINE_SCROLL_COARSE_STEPS);
            return;
        }
        final int nextOctave = Math.max(MIN_OCTAVE, Math.min(MAX_OCTAVE, getOctave() + amount));
        if (nextOctave == getOctave()) {
            return;
        }
        applyLayoutChange(() -> driver.setSharedOctave(nextOctave));
        showState("Octave");
    }

    private void scrollDrumMachineWindow(final int amount) {
        if (amount == 0) {
            return;
        }
        final int nextPosition = Math.max(0,
                Math.min(MAX_DRUM_MACHINE_SCROLL_POSITION, drumMachineScrollPosition + amount));
        if (nextPosition == drumMachineScrollPosition) {
            showDrumMachineWindowInfo();
            return;
        }
        drumMachineScrollPosition = nextPosition;
        liveDrumPadBank.scrollPosition().set(nextPosition);
        showDrumMachineWindowInfo();
    }

    private boolean canScrollDrumMachineWindow(final int amount) {
        if (amount == 0) {
            return false;
        }
        final int nextPosition = Math.max(0,
                Math.min(MAX_DRUM_MACHINE_SCROLL_POSITION, drumMachineScrollPosition + amount));
        return nextPosition != drumMachineScrollPosition;
    }

    private void showDrumMachineWindowInfo() {
        oled.valueInfo("Pad Low", formatDrumMachinePadWindowLowNote());
    }

    private String formatDrumMachinePadWindowLowNote() {
        return "P%d %s".formatted(relativeDrumMachinePadPage(), formatMidiNoteName(drumMachineScrollPosition));
    }

    private int relativeDrumMachinePadPage() {
        return Math.floorDiv(drumMachineScrollPosition - DEFAULT_DRUM_MACHINE_LOW_NOTE,
                DRUM_MACHINE_SCROLL_COARSE_STEPS) + 1;
    }

    private static String formatMidiNoteName(final int midiNote) {
        final int octave = midiNote / 12 - 2;
        return NoteGridLayout.noteName(midiNote) + octave;
    }

    private void showDrumMachineLayoutInfo() {
        oled.valueInfo("Drum Layout", drumMachineLayout.displayName());
    }

    private String formatSignedValue(final int value) {
        return value > 0 ? "+" + value : Integer.toString(value);
    }

    private void adjustTransposeSemitone(final int amount) {
        if (amount == 0) {
            return;
        }
        applyLayoutChange(() -> driver.adjustSharedRootNote(amount));
        showState("Root");
    }

    private void handleMainEncoder(final int inc) {
        if (driver.isPopupBrowserActive()) {
            driver.routeBrowserMainEncoder(inc);
            return;
        }
        driver.markMainEncoderTurned();
        if (driver.handleMainEncoderGlobalChord(inc)) {
            return;
        }
        if (noteRepeatHandler.getNoteRepeatActive().get()) {
            // While transient live note repeat is active, keep SELECT dedicated to repeat-rate edits.
            // Turning through the minimum rate should not drop straight into the underlying encoder role.
            noteRepeatHandler.handleMainEncoder(inc, driver.isGlobalAltHeld(), false);
            return;
        }
        final boolean fine = driver.isGlobalShiftHeld();
        final String mainEncoderRole = driver.getMainEncoderRolePreference();
        if (AkaiFireOikontrolExtension.MAIN_ENCODER_NOTE_REPEAT_ROLE.equals(mainEncoderRole)) {
            noteRepeatHandler.handleMainEncoder(inc, driver.isGlobalAltHeld());
        } else if (AkaiFireOikontrolExtension.MAIN_ENCODER_TEMPO_ROLE.equals(mainEncoderRole)) {
            driver.adjustTempo(inc, fine);
        } else if (AkaiFireOikontrolExtension.MAIN_ENCODER_SHUFFLE_ROLE.equals(mainEncoderRole)) {
            driver.adjustGrooveShuffleAmount(inc, fine);
        } else if (AkaiFireOikontrolExtension.MAIN_ENCODER_TRACK_SELECT_ROLE.equals(mainEncoderRole)) {
            driver.adjustSelectedTrack(inc, driver.isMainEncoderPressed());
        } else if (AkaiFireOikontrolExtension.MAIN_ENCODER_PLAYBACK_START_ROLE.equals(mainEncoderRole)) {
            driver.adjustPlaybackStartPositionByGrid(inc);
        } else if (AkaiFireOikontrolExtension.MAIN_ENCODER_DRUM_GRID_ROLE.equals(mainEncoderRole)) {
            oled.valueInfo("Drum Grid", "Drum only");
        } else {
            driver.adjustMainCursorParameter(inc, fine);
        }
    }

    private void handleMainEncoderPress(final boolean pressed) {
        if (driver.isPopupBrowserActive()) {
            driver.routeBrowserMainEncoderPress(pressed);
            return;
        }
        driver.setMainEncoderPressed(pressed);
        if (pressed && driver.isGlobalAltHeld()) {
            mainEncoderPressConsumed = true;
            driver.toggleCurrentDeviceWindow();
            return;
        }
        if (!pressed && mainEncoderPressConsumed) {
            mainEncoderPressConsumed = false;
            return;
        }
        if (noteRepeatHandler.getNoteRepeatActive().get()) {
            noteRepeatHandler.handlePressed(pressed);
            return;
        }
        if (pressed && driver.isGlobalShiftHeld()) {
            mainEncoderPressConsumed = true;
            driver.cycleMainEncoderRolePreference();
            return;
        }
        final String mainEncoderRole = driver.getMainEncoderRolePreference();
        if (!pressed && !driver.wasMainEncoderTurnedWhilePressed()) {
            driver.toggleMainEncoderRolePreference();
            return;
        }
        if (AkaiFireOikontrolExtension.MAIN_ENCODER_NOTE_REPEAT_ROLE.equals(mainEncoderRole)) {
            noteRepeatHandler.handlePressed(pressed);
        } else if (AkaiFireOikontrolExtension.MAIN_ENCODER_TEMPO_ROLE.equals(mainEncoderRole)) {
            if (pressed) {
                driver.showTempoInfo();
            } else {
                oled.clearScreenDelayed();
            }
        } else if (AkaiFireOikontrolExtension.MAIN_ENCODER_SHUFFLE_ROLE.equals(mainEncoderRole)) {
            if (pressed) {
                driver.showGrooveShuffleInfo();
            }
        } else if (AkaiFireOikontrolExtension.MAIN_ENCODER_TRACK_SELECT_ROLE.equals(mainEncoderRole)) {
            if (pressed) {
                driver.showSelectedTrackInfo(false);
            } else {
                oled.clearScreenDelayed();
            }
        } else if (AkaiFireOikontrolExtension.MAIN_ENCODER_PLAYBACK_START_ROLE.equals(mainEncoderRole)) {
            if (pressed) {
                oled.valueInfo("Play Start", "Grid step");
            } else {
                oled.clearScreenDelayed();
            }
        } else if (AkaiFireOikontrolExtension.MAIN_ENCODER_DRUM_GRID_ROLE.equals(mainEncoderRole)) {
            if (pressed) {
                oled.valueInfo("Drum Grid", "Drum only");
            } else {
                oled.clearScreenDelayed();
            }
        } else if (pressed) {
            driver.showMainCursorParameterInfo();
        }
    }

    private void applyLayout() {
        for (int i = 0; i < noteTranslationTable.length; i++) {
            noteTranslationTable[i] = -1;
        }
        noteInput.setKeyTranslationTable(noteTranslationTable);
    }

    private void applyLayoutChange(final Runnable stateChange) {
        retuneLivePads(stateChange);
    }

    private void releaseHeldLiveNotes() {
        livePadPerformer.releaseHeldNotes();
        clearHeldBongoPads();
    }

    private void clearHeldBongoPads() {
        for (int zone = 0; zone < heldBongoPads.length; zone++) {
            Arrays.fill(heldBongoPads[zone], false);
            heldBongoPadCounts[zone] = 0;
        }
    }

    private void retuneLivePads(final Runnable stateChange) {
        livePadPerformer.retuneHeldPads(() -> {
            stateChange.run();
            applyLayout();
        }, liveVelocity.centerVelocity());
    }

    private int[] getLivePadMidiNotes(final int padIndex) {
        final LiveNoteLayout layout = createLayout();
        if (layout instanceof DrumMachinePadLayout drumMachinePadLayout) {
            if (!liveDrumMachineDevice.hasDrumPads().get()) {
                return new int[0];
            }
            final int bankIndex = drumMachinePadLayout.padBankIndexForPad(padIndex);
            if (bankIndex < 0 || bankIndex >= drumMachinePadExists.length || !drumMachinePadExists[bankIndex]) {
                return new int[0];
            }
        }
        final int[] layoutNotes = layout.notesForPad(padIndex);
        final int[] shifted = new int[layoutNotes.length];
        int count = 0;
        for (final int layoutNote : layoutNotes) {
            final int shiftedNote = applyLivePitchOffset(layoutNote);
            if (shiftedNote >= 0) {
                shifted[count++] = shiftedNote;
            }
        }
        if (count == shifted.length) {
            return shifted;
        }
        final int[] compact = new int[count];
        System.arraycopy(shifted, 0, compact, 0, count);
        return compact;
    }

    private int resolveLivePadVelocity(final int padIndex, final int configuredVelocity, final int rawVelocity) {
        if (isDrumMachineLiveMode() && drumMachineLayout == DrumMachinePadLayout.Layout.VELOCITY) {
            return drumMachineFixedVelocityForPad(padIndex);
        }
        return liveVelocity.resolveVelocityFromCenter(configuredVelocity, rawVelocity);
    }

    private int resolveLivePadTimbre(final int padIndex) {
        if (!isDrumMachineLiveMode() || drumMachineLayout != DrumMachinePadLayout.Layout.BONGOS
                || bongoZoneForPad(padIndex) < 0) {
            return -1;
        }
        final int column = padIndex % NoteGridLayout.PAD_COLUMNS;
        final int rowFromBottom = NoteGridLayout.PAD_ROWS - 1 - (padIndex / NoteGridLayout.PAD_COLUMNS);
        final int surfaceStartColumn = column >= 11 ? 11 : 5;
        final double x = column - surfaceStartColumn;
        final double y = rowFromBottom;
        final double dx = Math.abs(x - 2.0);
        final double dy = Math.abs(y - 1.5);
        final double distance = Math.sqrt(dx * dx + dy * dy);
        return Math.max(MIN_MIDI_VALUE, Math.min(MAX_MIDI_VALUE, (int) Math.round(distance / 2.5 * MAX_MIDI_VALUE)));
    }

    private int drumMachineFixedVelocityForPad(final int padIndex) {
        final int column = padIndex % NoteGridLayout.PAD_COLUMNS;
        final int rowFromTop = padIndex / NoteGridLayout.PAD_COLUMNS;
        final int rowFromBottom = NoteGridLayout.PAD_ROWS - 1 - rowFromTop;
        if (column < 4) {
            return liveVelocity.centerVelocity();
        }
        final int columnTier = Math.min(3, (column - 4) / 3);
        return Math.max(MIN_VELOCITY, Math.min(MAX_MIDI_VALUE, 36 + rowFromBottom * 22 + columnTier * 8));
    }

    private void showLiveVelocityInfo() {
        if (driver.isGlobalShiftHeld()) {
            oled.valueInfo("Velocity Center", Integer.toString(liveVelocity.centerVelocity()));
            return;
        }
        oled.valueInfo("Velocity", liveVelocity.summary());
    }

    private RgbLigthState getPadLight(final int padIndex) {
        return getLivePadLight(padIndex);
    }

    private RgbLigthState getLivePadLight(final int padIndex) {
        final LiveNoteLayout layout = createLayout();
        if (isDrumMachineLiveMode() && layout instanceof DrumMachinePadLayout drumMachinePadLayout) {
            final RgbLigthState base = getDrumMachinePadBaseLight(padIndex, drumMachinePadLayout);
            return livePadPerformer.isPadHeld(padIndex) ? base.getBrightest() : base;
        }
        final int midiNote = applyLivePitchOffset(layout.primaryNoteForPad(padIndex));
        final RgbLigthState base;
        if (midiNote < 0) {
            base = RgbLigthState.OFF;
        } else if (isHarmonicLiveMode()) {
            base = getHarmonicLivePadBaseLight(padIndex, layout);
        } else {
            final NoteGridLayout.PadRole role = layout.roleForPad(padIndex);
            final RgbLigthState melodicFamilyColor = harmonicScaleFamilyColor();
            base = switch (role) {
                case ROOT -> ROOT_COLOR;
                case IN_SCALE -> melodicFamilyColor;
                case OUT_OF_SCALE -> OUT_OF_SCALE_COLOR;
                case UNAVAILABLE -> RgbLigthState.OFF;
            };
        }
        return livePadPerformer.isPadHeld(padIndex) ? base.getBrightest() : base;
    }

    private RgbLigthState getDrumMachinePadBaseLight(final int padIndex, final DrumMachinePadLayout layout) {
        if (!liveDrumMachineDevice.hasDrumPads().get()) {
            return RgbLigthState.OFF;
        }
        final int bankIndex = layout.padBankIndexForPad(padIndex);
        if (bankIndex < 0 || bankIndex >= drumMachinePadColors.length || !drumMachinePadExists[bankIndex]) {
            return RgbLigthState.OFF;
        }
        final RgbLigthState color = drumMachinePadColors[bankIndex];
        final RgbLigthState base = color != null ? color : trackPadColor();
        if (layout.selectorOffsetForPad(padIndex) == selectedDrumPadOffset) {
            return base.getBrightest();
        }
        if (drumMachineLayout == DrumMachinePadLayout.Layout.VELOCITY && layout.selectorOffsetForPad(padIndex) < 0) {
            return velocityRampColor(base, drumMachineFixedVelocityForPad(padIndex));
        }
        return base;
    }

    private RgbLigthState trackPadColor() {
        return trackBaseColor != null ? trackBaseColor : IN_SCALE_COLOR;
    }

    private RgbLigthState velocityRampColor(final RgbLigthState base, final int velocity) {
        if (velocity >= 112) {
            return base.getBrightest();
        }
        if (velocity >= 88) {
            return base.getBrightend();
        }
        if (velocity >= 64) {
            return base;
        }
        if (velocity >= 40) {
            return base.getSoftDimmed();
        }
        return base.getDimmed();
    }

    private RgbLigthState getHarmonicLivePadBaseLight(final int padIndex, final LiveNoteLayout layout) {
        final NoteGridLayout.PadRole role = layout.roleForPad(padIndex);
        final boolean bassColumnPad = harmonicBassColumns && (padIndex % NoteGridLayout.PAD_COLUMNS) < 2;
        if (role == NoteGridLayout.PadRole.UNAVAILABLE) {
            return RgbLigthState.OFF;
        }
        if (role == NoteGridLayout.PadRole.ROOT) {
            final int primaryMidiNote = applyLivePitchOffset(layout.primaryNoteForPad(padIndex));
            final RgbLigthState rootBase = bassColumnPad ? ROOT_COLOR.getSoftDimmed() : ROOT_COLOR;
            return livePadPerformer.isMidiNoteSounding(primaryMidiNote) ? rootBase.getBrightend() : rootBase;
        }
        final RgbLigthState familyColor = harmonicScaleFamilyColor();
        final RgbLigthState padBase = bassColumnPad ? familyColor.getSoftDimmed() : familyColor;
        final int primaryMidiNote = applyLivePitchOffset(layout.primaryNoteForPad(padIndex));
        if (livePadPerformer.isMidiNoteSounding(primaryMidiNote)) {
            return padBase.getBrightend();
        }
        return padBase;
    }

    private RgbLigthState harmonicScaleFamilyColor() {
        final String scaleName = getScale().getName().toLowerCase();
        if (scaleName.contains("ion")
                || scaleName.contains("major")
                || scaleName.contains("lydian")
                || scaleName.contains("mixolyd")) {
            return HARMONIC_BRIGHT_COLOR;
        }
        if (scaleName.contains("dorian")
                || scaleName.contains("aeolian")
                || scaleName.contains("minor")
                || scaleName.contains("melodic minor")
                || scaleName.contains("blues")) {
            return HARMONIC_MINOR_COLOR;
        }
        if (scaleName.contains("whole tone")
                || scaleName.contains("chromatic")
                || scaleName.contains("diminished")
                || scaleName.contains("octatonic")
                || scaleName.contains("augmented")) {
            return HARMONIC_SYMMETRIC_COLOR;
        }
        if (scaleName.contains("phryg")
                || scaleName.contains("locrian")
                || scaleName.contains("altered")
                || scaleName.contains("dominant")
                || scaleName.contains("double harmonic")) {
            return HARMONIC_TENSE_COLOR;
        }
        if (scaleName.contains("harmonic")
                || scaleName.contains("hungarian")
                || scaleName.contains("enigmatic")
                || scaleName.contains("persian")
                || scaleName.contains("byzantine")
                || scaleName.contains("oriental")) {
            return HARMONIC_EXOTIC_COLOR;
        }
        return HARMONIC_BRIGHT_COLOR;
    }

    private void showState(final String focus) {
        if ("Scale".equals(focus)) {
            oled.valueInfo("Scale", getScaleDisplayName());
            return;
        }
        if ("Root".equals(focus)) {
            oled.valueInfo("Root", "%s%d".formatted(NoteGridLayout.noteName(getRootNote()), getOctave()));
            return;
        }
        if ("Octave".equals(focus)) {
            if (isDrumMachineLiveMode()) {
                showDrumMachineWindowInfo();
                return;
            }
            oled.valueInfo("Octave", Integer.toString(getOctave()));
            return;
        }
        if ("Layout".equals(focus)) {
            if (isHarmonicLiveMode()) {
                oled.valueInfo("Layout", harmonicBassColumns ? "Bass Columns" : "Full Field");
            } else if (isDrumMachineLiveMode()) {
                showDrumMachineLayoutInfo();
            } else {
                oled.valueInfo("Layout", inKey ? "In Key" : "Chromatic");
            }
            return;
        }
        final String liveModeDetail;
        if (isHarmonicLiveMode()) {
            liveModeDetail = "Harmonic %s".formatted(harmonicBassColumns ? "Bass" : "Full");
        } else if (isDrumMachineLiveMode()) {
            liveModeDetail = drumMachineLayout.displayName();
        } else {
            liveModeDetail = inKey ? "In Key" : "Chromatic";
        }
        oled.lineInfo("Root %s%d".formatted(NoteGridLayout.noteName(getRootNote()), getOctave()),
                "Scale: %s\n%s".formatted(getScaleDisplayName(), liveModeDetail));
    }

    private MusicalScale getScale() {
        return pitchContext.getMusicalScale();
    }

    public MusicalScale getCurrentScale() {
        return getScale();
    }

    private String getScaleDisplayName() {
        return pitchContext.getShortScaleDisplayName();
    }

    public String getCurrentScaleDisplayName() {
        return getScaleDisplayName();
    }

    private int getRootNote() {
        return pitchContext.getRootNote();
    }

    public int getCurrentRootNoteClass() {
        return getRootNote();
    }

    private int getOctave() {
        return pitchContext.getOctave();
    }

    public int getCurrentOctave() {
        return getOctave();
    }

    public int getCurrentBaseMidiNote() {
        return pitchContext.getBaseMidiNote();
    }

    private int liveScaleMinIndex() {
        return 1;
    }

    private LiveNoteLayout createLayout() {
        if (isHarmonicLiveMode()) {
            return new HarmonicLatticeLayout(getScale(), getRootNote(), getOctave(),
                    HarmonicLatticeLayout.NOTE_COUNTS[harmonicNoteCountIndex],
                    harmonicOctaveSpan,
                    harmonicBassColumns, getHarmonicGlissStepOffset());
        }
        if (isDrumMachineLiveMode()) {
            return new DrumMachinePadLayout(drumMachineScrollPosition, drumMachineLayout,
                    selectedDrumPadOffset, secondaryDrumPadOffset);
        }
        return new NoteGridLayout(getScale(), getRootNote(), getOctave(), inKey);
    }

    private int applyLivePitchOffset(final int midiNote) {
        if (midiNote < 0) {
            return -1;
        }
        if (isHarmonicLiveMode() || isDrumMachineLiveMode()) {
            return midiNote;
        }
        if (livePitchGlissMode == LivePitchGlissMode.SCALE_DEGREE) {
            return pitchContext.transposeByScaleDegrees(midiNote, liveScaleDegreeGlissOffset);
        }
        final int shifted = midiNote + getLivePitchOffset();
        return shifted >= 0 && shifted <= 127 ? shifted : -1;
    }

    private void clearTranslation() {
        livePadPerformer.releaseHeldNotes();
        for (int i = 0; i < noteTranslationTable.length; i++) {
            noteTranslationTable[i] = -1;
        }
        noteInput.setKeyTranslationTable(noteTranslationTable);
    }

    private void applyLiveVelocity() {
        final Integer[] velocityTable = new Integer[128];
        for (int i = 0; i < velocityTable.length; i++) {
            velocityTable[i] = liveVelocity.centerVelocity();
        }
        noteInput.setVelocityTranslationTable(velocityTable);
        noteRepeatHandler.setNoteInputVelocity(liveVelocity.centerVelocity());
    }

    @Override
    protected void onActivate() {
        active = true;
        resetLiveIdleDisplay();
        notePlayController.activate();
        applyLiveFooterLegend();
        liveModeControlLayer.activate();
        if (drumPadsOnly && !drumMachineDefaultPageApplied) {
            drumMachineDefaultPageApplied = true;
            if (drumMachineScrollPosition == 0) {
                drumMachineScrollPosition = DEFAULT_DRUM_MACHINE_LOW_NOTE;
                liveDrumPadBank.scrollPosition().set(DEFAULT_DRUM_MACHINE_LOW_NOTE);
            }
        }
        activateDrumPadPatternButtons();
        applyLiveVelocity();
        applyLayout();
        showState("Mode");
    }

    @Override
    protected void onDeactivate() {
        clearStepInputHelper();
        active = false;
        resetLiveIdleDisplay();
        oled.setFooterLegend(null);
        clearHeldBongoPads();
        notePlayController.deactivate(this::releaseHeldLiveNotes);
        liveModeControlLayer.deactivate();
        clearTranslation();
        clearDrumPadPatternButtons();
    }

    private static NoteLiveEncoderModeControls.LayerHandle liveEncoderLayer(final Layer layer) {
        return new NoteLiveEncoderModeControls.LayerHandle() {
            @Override
            public void activate() {
                layer.activate();
            }

            @Override
            public void deactivate() {
                layer.deactivate();
            }
        };
    }
}
