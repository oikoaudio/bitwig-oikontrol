package com.oikoaudio.fire.sequence;

import com.bitwig.extension.controller.api.NoteOccurrence;
import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extensions.framework.Layer;
import com.oikoaudio.fire.AkaiFireOikontrolExtension;
import com.oikoaudio.fire.NoteAssign;
import com.oikoaudio.fire.control.BiColorButton;
import com.oikoaudio.fire.control.EncoderTurnBehavior;
import com.oikoaudio.fire.control.TouchEncoder;
import com.oikoaudio.fire.display.OledDisplay;
import com.oikoaudio.fire.lights.BiColorLightState;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared encoder-page layer for step-sequencer-style modes. It manages encoder bank switching,
 * touch gestures, OLED feedback, and the wiring between encoder slots and note or mixer parameters.
 */
public class StepSequencerEncoderLayer extends Layer {
    private static final long MODE_INFO_HOLD_MS = 2000L;
    private final StepSequencerHost parent;
    private final AkaiFireOikontrolExtension driver;
    private final EncoderBankLayout layout;

    private EncoderMode encoderMode = EncoderMode.CHANNEL;
    private final Layer channelLayer;
    private final Layer mixerLayer;
    private final Layer user1Layer;
    private final Layer user2Layer;

    private Layer currentLayer;
    private final OledDisplay oled;
    private final Map<EncoderMode, Layer> modeMapping = new HashMap<>();
    private final TouchEncoder[] encoders;

    @FunctionalInterface
    interface NoteDoubleGetter {
        double get(NoteStep step);
    }

    @FunctionalInterface
    interface NoteDoubleSetter {
        void set(NoteStep step, double value);
    }

    @FunctionalInterface
    interface NoteIntGetter {
        int get(NoteStep step);
    }

    @FunctionalInterface
    interface NoteIntSetter {
        void set(NoteStep step, int value);
    }

    /** Handles a mode-local insertion default when no existing note is targeted. */
    public interface EmptyNoteAccessHandler {
        void adjust(int amount);

        void show();

        void reset();
    }

    public StepSequencerEncoderLayer(
            final StepSequencerHost host,
            final AkaiFireOikontrolExtension driver,
            final EncoderBankLayout layout) {
        super(driver.getLayers(), "Encoder_layer");
        this.parent = host;
        this.driver = driver;
        this.layout = layout;
        this.oled = driver.getOled();
        channelLayer = new Layer(driver.getLayers(), "ENC_CHANNEL_LAYER");
        mixerLayer = new Layer(driver.getLayers(), "ENC_MIXER_LAYER");
        user1Layer = new Layer(driver.getLayers(), "ENC_USER1_LAYER");
        user2Layer = new Layer(driver.getLayers(), "ENC_USER2_LAYER");
        encoders = driver.getEncoders();
        bindBank(EncoderMode.CHANNEL, channelLayer, layout.bank(EncoderMode.CHANNEL));
        bindBank(EncoderMode.MIXER, mixerLayer, layout.bank(EncoderMode.MIXER));
        bindBank(EncoderMode.USER_1, user1Layer, layout.bank(EncoderMode.USER_1));
        bindBank(EncoderMode.USER_2, user2Layer, layout.bank(EncoderMode.USER_2));
        currentLayer = channelLayer;
        final BiColorButton modeButon = driver.getButton(NoteAssign.KNOB_MODE);
        modeButon.bindPressed(this, this::handleModeAdvance, this::modeToLight);
    }

    private void bindBank(final EncoderMode mode, final Layer layer, final EncoderBank bank) {
        modeMapping.put(mode, layer);
        final EncoderSlotBinding[] slots = bank.slots();
        for (int i = 0; i < slots.length; i++) {
            encoders[i].setStepSize(slots[i].stepSize());
            slots[i].bind(this, layer, encoders[i], i);
        }
    }

    public EncoderMode nextMode() {
        if (encoderMode == EncoderMode.CHANNEL) {
            return EncoderMode.MIXER;
        } else if (encoderMode == EncoderMode.MIXER) {
            return EncoderMode.USER_1;
        } else if (encoderMode == EncoderMode.USER_1) {
            return EncoderMode.USER_2;
        }
        return EncoderMode.CHANNEL;
    }

    public void toggleShiftForCurrentMode() {
        // No alternate page variants in the shared step-page model.
    }

    public void bindNoteAccess(
            final Layer layer,
            final TouchEncoder encoder,
            final int slotIndex,
            final NoteStepAccess access) {
        bindNoteAccess(layer, encoder, slotIndex, access, null);
    }

    public void bindNoteAccess(
            final Layer layer,
            final TouchEncoder encoder,
            final int slotIndex,
            final NoteStepAccess access,
            final EmptyNoteAccessHandler emptyHandler) {
        final EncoderTurnBehavior behavior =
                !access.usesAcceleratedTurnBehavior()
                        ? EncoderTurnBehavior.quantizedSteps(access.getStepThreshold())
                        : EncoderTurnBehavior.acceleratedValue(access.accelerationProfile());
        encoder.bindEncoderBehavior(
                layer,
                driver::isGlobalShiftHeld,
                behavior,
                effectiveInc -> {
                    if (effectiveInc != 0) {
                        if (parent.handleNoteVariationTurn(access, effectiveInc)) {
                            return;
                        }
                        final List<NoteStep> notes = activeNotesForAccess();
                        if (notes.isEmpty() && emptyHandler != null) {
                            emptyHandler.adjust(effectiveInc);
                        } else {
                            handleMod(effectiveInc, access, notes);
                        }
                    }
                });
        encoder.bindTouched(
                layer,
                touched -> {
                    if (!touched) {
                        behavior.reset();
                    }
                    if (parent.handleNoteVariationTouch(access, touched)) {
                        return;
                    }
                    handleTouch(slotIndex, touched, access, emptyHandler);
                });
    }

    public void handleExplicitNoteAccess(final int inc, final NoteStepAccess accessor) {
        handleMod(inc, accessor);
    }

    public void handleExplicitNoteAccess(
            final int inc, final NoteStepAccess accessor, final List<NoteStep> notes) {
        handleMod(inc, accessor, notes);
    }

    public void showAccessorTouchValue(final NoteStepAccess accessor) {
        showTouchPress(accessor);
    }

    public void showAccessorTouchValue(final NoteStepAccess accessor, final List<NoteStep> notes) {
        showTouchPress(accessor, notes);
    }

    public boolean resetAccessorToDefault(final NoteStepAccess accessor) {
        if (!accessor.canReset()) {
            return false;
        }
        final List<NoteStep> activeNotes = activeNotesForAccess();
        return resetAccessorToDefault(accessor, activeNotes);
    }

    public boolean resetAccessorToDefault(
            final NoteStepAccess accessor, final List<NoteStep> activeNotes) {
        if (!accessor.canReset()) {
            return false;
        }
        if (activeNotes.isEmpty()) {
            return false;
        }
        accessor.applyReset(parent, activeNotes);
        showCurrentValue(accessor, activeNotes);
        parent.registerModifiedSteps(activeNotes);
        return true;
    }

    private void handleModeAdvance(final boolean pressed) {
        if (pressed) {
            return;
        }
        if (driver.consumeKnobModeGesture()) {
            oled.clearScreenDelayed();
            return;
        }
        if (parent.isSelectHeld()) { // display encoder details on select + mode
            applyFooterLegend();
            oled.detailInfo("Encoder Mode", parent.getModeInfo(encoderMode));
        } else {
            switchMode(nextMode());
        }
    }

    private void switchMode(final EncoderMode newMode) {
        encoderMode = newMode;
        currentLayer.deactivate();
        currentLayer = modeMapping.get(encoderMode);
        currentLayer.activate();
        applyResolution(encoderMode);
        parent.handleEncoderModeChanged(encoderMode);
        applyFooterLegend();
        oled.detailInfo("Encoder Mode", parent.getModeInfo(encoderMode));
        oled.clearScreenDelayed(MODE_INFO_HOLD_MS);
    }

    private void applyResolution(final EncoderMode mode) {
        final EncoderSlotBinding[] slots = layout.bank(mode).slots();
        for (int i = 0; i < slots.length; i++) {
            encoders[i].setStepSize(slots[i].stepSize());
        }
    }

    private BiColorLightState modeToLight() {
        return encoderMode.getState();
    }

    public EncoderMode getEncoderMode() {
        return encoderMode;
    }

    private void handleMod(final int inc, final NoteStepAccess accessor) {
        final List<NoteStep> notes = activeNotesForAccess();
        handleMod(inc, accessor, notes);
    }

    private void handleMod(
            final int inc, final NoteStepAccess accessor, final List<NoteStep> notes) {
        if (notes.isEmpty()) {
            return;
        }
        for (int i = 0; i < notes.size(); i++) {
            final NoteStep note = notes.get(i);

            final String function =
                    parent.isPadBeingHeld() ? "ALL " + accessor.getName() : accessor.getName();
            final String details = parent.getDetails(notes);
            final boolean first = i == 0;

            if (accessor.getUnit() == NoteValueUnit.MIDI
                    || accessor.getUnit() == NoteValueUnit.NONE) {
                final Integer newValue = accessor.applyIntIncrement(inc, note);
                if (first && newValue != null) {
                    oled.paramInfo(
                            function,
                            newValue,
                            details,
                            accessor.getMinInt(),
                            accessor.getMaxInt());
                }
            } else if (accessor.getUnit() == NoteValueUnit.OCCURENCE) {
                final NoteOccurrence newValue = incrementOccurence(inc, note);
                if (newValue != null) {
                    oled.paramInfo(function, newValue.toString().replace("_", " "), details);
                }
            } else if (accessor.getUnit() == NoteValueUnit.RECURRENCE) {
                final Integer newValue = accessor.applyIntIncrement(inc, note);
                if (first && newValue != null) {
                    parent.updateRecurrenceLength(newValue);
                    oled.paramInfo(
                            function,
                            newValue,
                            details,
                            accessor.getMinInt(),
                            accessor.getMaxInt(),
                            1);
                }
            } else {
                handleIncDouble(inc, accessor, notes, note, first);
            }
        }
        parent.registerModifiedSteps(notes);
    }

    private NoteOccurrence incrementOccurence(final int inc, final NoteStep note) {
        final NoteOccurrence occurrence = note.occurrence();
        final NoteOccurrence[] vs = NoteOccurrence.values();
        int index = -1;
        for (int en = 0; en < vs.length; en++) {
            if (occurrence == vs[en]) {
                index = en;
                break;
            }
        }
        final int next = index + inc;
        if (next >= 0 && next < vs.length) {
            final NoteOccurrence newValue = vs[next];
            note.setOccurrence(newValue);
            return newValue;
        }
        return null;
    }

    private void handleIncDouble(
            final int inc,
            final NoteStepAccess accessor,
            final List<NoteStep> notes,
            final NoteStep note,
            final boolean print) {
        Double newValue = null;
        if (accessor.getUnit() == NoteValueUnit.NOTE_LEN) {
            final double stepLen = note.duration() / parent.getGridResolution();

            final double newStepLen = incrementStepLength(inc, stepLen, 0.1, 16.0);
            if (newStepLen != stepLen) {
                newValue = newStepLen * parent.getGridResolution();
                note.setDuration(newValue);
            }
        } else {
            newValue = accessor.applyDoubleIncrement(inc, note);
        }

        if (print && newValue != null) {
            final String details = parent.getDetails(notes);
            showDoubleValue(accessor, newValue, details);
        }
    }

    private double incrementStepLength(
            final int inc, final double stepLen, final double min, final double max) {
        double newStepLength = 0;
        if (stepLen <= 1.0) {
            newStepLength = incStep(inc, stepLen, 0.01);
        } else if (stepLen <= 2.0) {
            newStepLength = incStep(inc, stepLen, 0.02);
        } else if (stepLen <= 4.0) {
            newStepLength = incStep(inc, stepLen, 0.05);
        } else {
            newStepLength = incStep(inc, stepLen, 0.1);
        }
        if (newStepLength < min) {
            return min;
        } else if (newStepLength > max) {
            return max;
        }

        return newStepLength;
    }

    private double incStep(final int inc, final double stepLen, final double amount) {
        double newStepLength;
        newStepLength = stepLen + amount * inc;
        // TODO figure out snapping
        //		final double roundValue = Math.round(newStepLength);
        //		final double diff = Math.abs(newStepLength - roundValue);
        //		if (diff < amount) {
        //			return roundValue;
        //		}
        return newStepLength;
    }

    private void showDoubleValue(
            final NoteStepAccess accessor, final Double value, final String details) {
        if (accessor.getUnit() == NoteValueUnit.SEMI) {
            oled.paramInfoDouble(
                    accessor.getName(), value, details, accessor.getMin(), accessor.getMax());
        } else if (accessor.getUnit() == NoteValueUnit.NOTE_LEN) {
            oled.paramInfoDuration(accessor.getName(), value, details, parent.getGridResolution());
        } else {
            oled.paramInfoPercent(
                    accessor.getName(), value, details, accessor.getMin(), accessor.getMax());
        }
    }

    private void handleTouch(
            final int slotIndex,
            final boolean touched,
            final NoteStepAccess accessor,
            final EmptyNoteAccessHandler emptyHandler) {
        if (touched) {
            if (activeNotesForAccess().isEmpty() && emptyHandler != null) {
                if (driver.handleKnobModeEncoderReset(
                        true,
                        true,
                        accessor.getName(),
                        "No reset",
                        emptyHandler::reset,
                        emptyHandler::show)) {
                    return;
                }
                emptyHandler.show();
                return;
            }
            if (driver.handleKnobModeEncoderReset(
                    true,
                    accessor.canReset(),
                    accessor.getName(),
                    "No reset",
                    () -> resetAccessorToDefault(accessor),
                    () -> showTouchPress(accessor))) {
                return;
            }
            showTouchPress(accessor);
            return;
        }
        oled.clearScreenDelayed();
        if (accessor.getUnit() == NoteValueUnit.RECURRENCE) {
            parent.exitRecurrenceEdit();
        } else if (accessor.getUnit() == NoteValueUnit.NOTE_LEN) {
            parent.getLengthDisplay().set(false);
        }
    }

    private void showTouchPress(final NoteStepAccess accessor) {
        final List<NoteStep> activeNotes = activeNotesForAccess();
        showTouchPress(accessor, activeNotes);
    }

    private void showTouchPress(final NoteStepAccess accessor, final List<NoteStep> activeNotes) {
        if (parent.getDeleteHeld().get() && accessor.canReset()) {
            accessor.applyReset(parent, activeNotes);
            oled.paramInfo("Reset:" + accessor.getName(), parent.getPadInfo());
            return;
        }
        if (activeNotes.isEmpty()) {
            if (accessor.getUnit() == NoteValueUnit.OCCURENCE
                    || accessor.getUnit() == NoteValueUnit.RECURRENCE) {
                oled.valueInfo(accessor.getName(), "Hold Step");
            } else {
                oled.paramInfo(accessor.getName(), parent.getPadInfo());
            }
            return;
        }
        showCurrentValue(accessor, activeNotes);
        parent.registerModifiedSteps(activeNotes);
    }

    private List<NoteStep> activeNotesForAccess() {
        return parent.isPadBeingHeld() ? parent.getOnNotes() : parent.getFocusedNotes();
    }

    private void showCurrentValue(final NoteStepAccess accessor, final List<NoteStep> notes) {
        final NoteStep note = notes.get(0);
        final String details = parent.getDetails(notes);
        if (accessor.getUnit() == NoteValueUnit.NOTE_LEN) {
            parent.getLengthDisplay().set(true);
        }
        if (accessor.getUnit() == NoteValueUnit.MIDI || accessor.getUnit() == NoteValueUnit.NONE) {
            oled.paramInfo(
                    accessor.getName(),
                    accessor.getInt(note),
                    details,
                    accessor.getMinInt(),
                    accessor.getMaxInt());
        } else if (accessor.getUnit() == NoteValueUnit.OCCURENCE) {
            oled.paramInfo(
                    accessor.getName(), note.occurrence().toString().replace("_", " "), details);
        } else if (accessor.getUnit() == NoteValueUnit.RECURRENCE) {
            oled.paramInfo(
                    accessor.getName(),
                    accessor.getInt(note),
                    details,
                    accessor.getMinInt(),
                    accessor.getMaxInt());
            parent.enterRecurrenceEdit(notes);
        } else {
            showDoubleValue(accessor, accessor.getDouble(note), details);
        }
    }

    @Override
    protected void onActivate() {
        currentLayer.activate();
        applyResolution(encoderMode);
        applyFooterLegend();
    }

    @Override
    protected void onDeactivate() {
        currentLayer.deactivate();
        oled.setFooterLegend(null);
    }

    private void applyFooterLegend() {
        oled.setFooterLegend(parent.getEncoderFooterLegend(encoderMode));
    }
}
