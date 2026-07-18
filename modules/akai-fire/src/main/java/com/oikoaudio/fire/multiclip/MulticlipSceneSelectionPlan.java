package com.oikoaudio.fire.multiclip;

import java.util.ArrayList;
import java.util.List;

final class MulticlipSceneSelectionPlan {
    private MulticlipSceneSelectionPlan() {}

    static List<Integer> missingEligibleLanes(
            final boolean[] eligible, final boolean[] hasContent) {
        if (eligible.length != hasContent.length) {
            throw new IllegalArgumentException("Lane state arrays must have equal lengths");
        }
        final List<Integer> missing = new ArrayList<>();
        for (int position = 0; position < eligible.length; position++) {
            if (eligible[position] && !hasContent[position]) {
                missing.add(position);
            }
        }
        return List.copyOf(missing);
    }
}
