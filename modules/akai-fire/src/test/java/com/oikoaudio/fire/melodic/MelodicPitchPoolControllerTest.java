package com.oikoaudio.fire.melodic;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MelodicPitchPoolControllerTest {
    private static final class AuditionPort implements MelodicPitchPoolController.AuditionPort {
        private boolean enabled = true;
        private String events = "";

        @Override public boolean enabled() { return enabled; }
        @Override public void noteOn(final int pitch) { events += "on:" + pitch + ";"; }
        @Override public void noteOff(final int pitch) { events += "off:" + pitch + ";"; }
    }

    @Test
    void togglesMembershipAndTracksUserEditsVersusGeneratorSource() {
        final MelodicPitchPoolController pool = new MelodicPitchPoolController(new AuditionPort());
        pool.replaceGenerated(List.of(48, 52, 55), "acid");
        assertTrue(pool.contains(52));
        assertTrue(pool.generatedBy("acid"));

        assertFalse(pool.toggleMembership(52));
        assertTrue(pool.userEdited());
        assertFalse(pool.generatedBy("acid"));
    }

    @Test
    void auditionIsDeduplicatedAndAlwaysCleanedUp() {
        final AuditionPort port = new AuditionPort();
        final MelodicPitchPoolController pool = new MelodicPitchPoolController(port);

        pool.startAudition(60);
        pool.startAudition(60);
        pool.stopAudition(60);
        pool.startAudition(62);
        pool.stopAllAuditions();

        assertEquals("on:60;off:60;on:62;off:62;", port.events);
    }

    @Test
    void invalidOrDisabledAuditionsProduceNoMidi() {
        final AuditionPort port = new AuditionPort();
        port.enabled = false;
        final MelodicPitchPoolController pool = new MelodicPitchPoolController(port);
        pool.startAudition(60);
        port.enabled = true;
        pool.startAudition(128);
        assertEquals("", port.events);
    }

    @Test
    void heldStepPressAssignsWithoutChangingMembership() {
        final AuditionPort port = new AuditionPort();
        final MelodicPitchPoolController pool = new MelodicPitchPoolController(port);
        pool.replaceGenerated(List.of(60), "motif");
        final String[] assignment = {""};

        assertEquals(MelodicPitchPoolController.PressResult.ASSIGNED,
                pool.pressPitch(64, 7, (step, pitch) -> assignment[0] = step + ":" + pitch));
        assertEquals("7:64", assignment[0]);
        assertEquals(List.of(60), List.copyOf(pool.pitches()));
    }
}
