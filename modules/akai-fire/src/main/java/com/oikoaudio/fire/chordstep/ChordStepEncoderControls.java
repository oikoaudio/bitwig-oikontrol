package com.oikoaudio.fire.chordstep;

import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extensions.framework.Layer;
import com.oikoaudio.fire.AkaiFireOikontrolExtension;
import com.oikoaudio.fire.control.EncoderStepAccumulator;
import com.oikoaudio.fire.control.EncoderValueProfile;
import com.oikoaudio.fire.control.MixerEncoderProfile;
import com.oikoaudio.fire.control.ParameterEncoderBinding;
import com.oikoaudio.fire.control.TouchEncoder;
import com.oikoaudio.fire.display.EncoderFooterLegend;
import com.oikoaudio.fire.display.OledDisplay;
import com.oikoaudio.fire.sequence.EncoderBank;
import com.oikoaudio.fire.sequence.EncoderBankLayout;
import com.oikoaudio.fire.sequence.EncoderMode;
import com.oikoaudio.fire.sequence.EncoderSlotBinding;
import com.oikoaudio.fire.sequence.NoteStepAccess;
import com.oikoaudio.fire.sequence.StepSequencerEncoderLayer;

import java.util.EnumMap;
import java.util.Map;

final class ChordStepEncoderControls {
    private static final int CHORD_ROOT_ENCODER_THRESHOLD = 16;
    private static final int CHORD_OCTAVE_ENCODER_THRESHOLD = 8;
    private static final int CHORD_FAMILY_ENCODER_THRESHOLD = 8;

    private final AkaiFireOikontrolExtension driver;
    private final OledDisplay oled;
    private final CursorTrack cursorTrack;
    private final Host host;
    private final EncoderStepAccumulator chordRootEncoder = new EncoderStepAccumulator(CHORD_ROOT_ENCODER_THRESHOLD);
    private final EncoderStepAccumulator chordOctaveEncoder = new EncoderStepAccumulator(CHORD_OCTAVE_ENCODER_THRESHOLD);
    private final EncoderStepAccumulator chordFamilyEncoder = new EncoderStepAccumulator(CHORD_FAMILY_ENCODER_THRESHOLD);
    private final EncoderBankLayout layout;
    private boolean mainEncoderPressConsumed = false;

    ChordStepEncoderControls(final AkaiFireOikontrolExtension driver,
                             final OledDisplay oled,
                             final CursorTrack cursorTrack,
                             final Host host) {
        this.driver = driver;
        this.oled = oled;
        this.cursorTrack = cursorTrack;
        this.host = host;
        this.layout = createLayout();
    }

    EncoderBankLayout layout() {
        return layout;
    }

    void bindMainEncoder(final Layer layer) {
        final TouchEncoder mainEncoder = driver.getMainEncoder();
        mainEncoder.bindEncoder(layer, this::handleMainEncoder);
        mainEncoder.bindTouched(layer, this::handleMainEncoderPress);
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
        final boolean fine = driver.isGlobalShiftHeld();
        final String mainEncoderRole = driver.getMainEncoderRolePreference();
        if (driver.routeGlobalMainEncoderRole(inc, fine)) {
            return;
        } else if (AkaiFireOikontrolExtension.MAIN_ENCODER_NOTE_REPEAT_ROLE.equals(mainEncoderRole)) {
            oled.valueInfo("Note Repeat", "Live only");
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
            if (pressed) {
                oled.valueInfo("Note Repeat", "Live only");
            }
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

    private EncoderBankLayout createLayout() {
        final Map<EncoderMode, EncoderBank> banks = new EnumMap<>(EncoderMode.class);
        banks.put(EncoderMode.CHANNEL, new EncoderBank(
                "1: Oct/Root/Scale\n2: Velocity\n3: Chord Family\n4: Int/Inv/Layout",
                EncoderFooterLegend.of("RScO", "Velo", "Chrd", "Intr"),
                new EncoderSlotBinding[]{
                        chordPitchContextSlot(),
                        chordBuildVelocitySlot(),
                        chordSlot(2, chordFamilyEncoder,
                                amount -> {
                                    if (driver.isGlobalAltHeld()) {
                                        host.adjustChordPage(amount);
                                    } else {
                                        host.adjustChordFamily(amount);
                                    }
                                },
                                host::showCurrentChord, host::resetChordFamilySelection),
                        interpretSlot()
                }));
        banks.put(EncoderMode.MIXER, new EncoderBank(
                "1: Volume\n2: Pan\n3: Send 1\n4: Send 2",
                new EncoderSlotBinding[]{
                        chordMixerSlot(0, "Volume"),
                        chordMixerSlot(1, "Pan"),
                        chordMixerSlot(2, "Send 1"),
                        chordMixerSlot(3, "Send 2")
                }));
        banks.put(EncoderMode.USER_1, new EncoderBank(
                "1: Velocity\n2: Pressure\n3: Timbre\n4: Pitch Expr",
                EncoderFooterLegend.of("Velo", "Pres", "Timb", "PExp"),
                new EncoderSlotBinding[]{
                        noteAccessSlot(NoteStepAccess.VELOCITY),
                        noteAccessSlot(NoteStepAccess.PRESSURE),
                        noteAccessSlot(NoteStepAccess.TIMBRE),
                        noteAccessSlot(NoteStepAccess.PITCH)
                }));
        banks.put(EncoderMode.USER_2, new EncoderBank(
                "1: Note Length\n2: Chance\n3: Vel Spread\n4: Repeat",
                EncoderFooterLegend.of("Len", "Chnc", "VSpr", "Rpt"),
                new EncoderSlotBinding[]{
                        noteAccessSlot(NoteStepAccess.DURATION),
                        noteAccessSlot(NoteStepAccess.CHANCE),
                        noteAccessSlot(NoteStepAccess.VELOCITY_SPREAD),
                        noteAccessSlot(NoteStepAccess.REPEATS)
                }));
        return new EncoderBankLayout(banks);
    }

    private EncoderSlotBinding noteAccessSlot(final NoteStepAccess access) {
        return new EncoderSlotBinding() {
            @Override
            public double stepSize() {
                return access.getResolution();
            }

            @Override
            public void bind(final StepSequencerEncoderLayer handler, final Layer layer, final TouchEncoder encoder,
                             final int slotIndex) {
                handler.bindNoteAccess(layer, encoder, slotIndex, access);
            }
        };
    }

    private EncoderSlotBinding chordPitchContextSlot() {
        return new EncoderSlotBinding() {
            @Override
            public double stepSize() {
                return 0.25;
            }

            @Override
            public void bind(final StepSequencerEncoderLayer handler, final Layer layer, final TouchEncoder encoder,
                             final int slotIndex) {
                encoder.bindEncoder(layer, inc -> {
                    final boolean scaleContext = driver.isGlobalShiftHeld() && !driver.isGlobalAltHeld();
                    if (scaleContext) {
                        if (inc != 0) {
                            host.adjustChordSharedScale(inc);
                        }
                        return;
                    }
                    final boolean rootContext = driver.isGlobalAltHeld();
                    final EncoderStepAccumulator accumulator = rootContext ? chordRootEncoder : chordOctaveEncoder;
                    final int amount = accumulator.consume(inc);
                    if (amount == 0) {
                        return;
                    }
                    if (rootContext) {
                        host.adjustChordRoot(amount);
                    } else {
                        host.adjustChordOctave(amount);
                    }
                });
                encoder.bindTouched(layer, touched -> {
                    if (touched) {
                        final boolean scaleContext = driver.isGlobalShiftHeld() && !driver.isGlobalAltHeld();
                        if (scaleContext) {
                            if (driver.handleKnobModeEncoderReset(true, false, "Scale", "No reset",
                                    () -> { }, () -> oled.valueInfo("Scale", host.getScaleDisplayName()))) {
                                return;
                            }
                            oled.valueInfo("Scale", host.getScaleDisplayName());
                            return;
                        }
                        final boolean rootContext = driver.isGlobalAltHeld();
                        if (driver.handleKnobModeEncoderReset(true, true, rootContext ? "Root" : "Octave",
                                "No reset", () -> {
                                    (rootContext ? chordRootEncoder : chordOctaveEncoder).reset();
                                    if (rootContext) {
                                        host.resetChordRoot();
                                    } else {
                                        host.resetChordOctave();
                                    }
                                }, () -> {
                                    if (rootContext) {
                                        host.showChordRootInfo();
                                    } else {
                                        host.showChordOctaveInfo();
                                    }
                                })) {
                            return;
                        }
                        if (rootContext) {
                            host.showChordRootInfo();
                        } else {
                            host.showChordOctaveInfo();
                        }
                        return;
                    }
                    oled.clearScreenDelayed();
                });
            }
        };
    }

    private EncoderSlotBinding chordBuildVelocitySlot() {
        return new EncoderSlotBinding() {
            @Override
            public double stepSize() {
                return MixerEncoderProfile.STEP_SIZE;
            }

            @Override
            public void bind(final StepSequencerEncoderLayer handler, final Layer layer, final TouchEncoder encoder,
                             final int slotIndex) {
                encoder.bindEncoder(layer, inc -> {
                    if (driver.isGlobalShiftHeld()) {
                        host.adjustChordVelocityCenter(inc);
                    } else {
                        host.adjustChordVelocitySensitivity(inc);
                    }
                });
                encoder.bindTouched(layer, touched -> {
                    if (touched) {
                        if (driver.handleKnobModeEncoderReset(true, true, "Velocity", "No reset",
                                host::resetChordVelocityTargets, host::showChordVelocityInfo)) {
                            return;
                        }
                        host.showChordVelocityInfo();
                        return;
                    }
                    oled.clearScreenDelayed();
                });
            }
        };
    }

    private EncoderSlotBinding chordMixerSlot(final int index, final String label) {
        return new EncoderSlotBinding() {
            @Override
            public double stepSize() {
                return MixerEncoderProfile.STEP_SIZE;
            }

            @Override
            public void bind(final StepSequencerEncoderLayer handler, final Layer layer, final TouchEncoder encoder,
                             final int slotIndex) {
                final Parameter parameter = switch (index) {
                    case 0 -> cursorTrack.volume();
                    case 1 -> cursorTrack.pan();
                    case 2 -> cursorTrack.sendBank().getItemAt(0);
                    default -> cursorTrack.sendBank().getItemAt(1);
                };
                final EncoderValueProfile profile = index == 1
                        ? EncoderValueProfile.PAN
                        : EncoderValueProfile.LARGE_RANGE;
                ParameterEncoderBinding.bind(encoder, layer, slotIndex, parameter, label, driver::isGlobalShiftHeld,
                        mixerResetPolicy(index), driver.knobModeEncoderResetControl(), profile, index == 1,
                        oled::valueInfoWithBar, oled::clearScreenDelayed);
            }
        };
    }

    private ParameterEncoderBinding.ResetPolicy mixerResetPolicy(final int index) {
        return ParameterEncoderBinding.ResetPolicy.PARAMETER_DEFAULT;
    }

    private EncoderSlotBinding chordSlot(final int slotIndex, final EncoderStepAccumulator accumulator,
                                         final ChordAdjuster adjuster, final Runnable showInfo,
                                         final Runnable resetAction) {
        return new EncoderSlotBinding() {
            @Override
            public double stepSize() {
                return 0.25;
            }

            @Override
            public void bind(final StepSequencerEncoderLayer handler, final Layer layer, final TouchEncoder encoder,
                             final int boundSlotIndex) {
                encoder.bindEncoder(layer, inc -> {
                    final int amount = accumulator.consume(inc);
                    if (amount != 0) {
                        adjuster.adjust(amount);
                    }
                });
                encoder.bindTouched(layer, touched -> {
                    if (touched) {
                        if (driver.handleKnobModeEncoderReset(true, true, "Chord", "No reset", () -> {
                            accumulator.reset();
                            resetAction.run();
                        }, showInfo)) {
                            return;
                        }
                        showInfo.run();
                        return;
                    }
                    oled.clearScreenDelayed();
                });
            }
        };
    }

    private EncoderSlotBinding interpretSlot() {
        return new EncoderSlotBinding() {
            @Override
            public double stepSize() {
                return 1.0;
            }

            @Override
            public void bind(final StepSequencerEncoderLayer handler, final Layer layer, final TouchEncoder encoder,
                             final int slotIndex) {
                encoder.bindEncoder(layer, inc -> {
                    if (inc != 0) {
                        if (driver.isGlobalShiftHeld() && !driver.isGlobalAltHeld()) {
                            host.setBuilderLayoutInKey(inc > 0);
                        } else if (driver.isGlobalAltHeld()) {
                            host.invertCurrentChord(inc > 0 ? 1 : -1);
                        } else {
                            host.adjustChordInterpretation(inc);
                        }
                    }
                });
                encoder.bindTouched(layer, touched -> {
                    if (touched) {
                        if (driver.isGlobalShiftHeld() && !driver.isGlobalAltHeld()) {
                            if (driver.handleKnobModeEncoderReset(true, false, "Builder Layout", "No reset",
                                    () -> { }, host::showBuilderLayoutInfo)) {
                                return;
                            }
                            host.showBuilderLayoutInfo();
                            return;
                        }
                        if (driver.isGlobalAltHeld()) {
                            if (driver.handleKnobModeEncoderReset(true, false, "Invert", "No reset",
                                    () -> { }, () -> oled.valueInfo("Invert", "Turn encoder"))) {
                                return;
                            }
                            oled.valueInfo("Invert", "Turn encoder");
                            return;
                        }
                        if (driver.handleKnobModeEncoderReset(true, true, "Interpret", "No reset",
                                host::resetChordInterpretation, host::showChordInterpretationInfo)) {
                            return;
                        }
                        host.showChordInterpretationInfo();
                        return;
                    }
                    oled.clearScreenDelayed();
                });
            }
        };
    }

    @FunctionalInterface
    private interface ChordAdjuster {
        void adjust(int amount);
    }

    interface Host {
        void adjustChordRoot(int amount);

        void resetChordRoot();

        void showChordRootInfo();

        void adjustChordOctave(int amount);

        void resetChordOctave();

        void showChordOctaveInfo();

        void adjustChordVelocityCenter(int inc);

        void adjustChordVelocitySensitivity(int inc);

        void resetChordVelocityTargets();

        void showChordVelocityInfo();

        void adjustChordPage(int amount);

        void adjustChordFamily(int amount);

        void showCurrentChord();

        void resetChordFamilySelection();

        void adjustChordSharedScale(int amount);

        String getScaleDisplayName();

        void invertCurrentChord(int direction);

        void adjustChordInterpretation(int amount);

        void setBuilderLayoutInKey(boolean inKey);

        void showBuilderLayoutInfo();

        void resetChordInterpretation();

        void showChordInterpretationInfo();
    }
}
