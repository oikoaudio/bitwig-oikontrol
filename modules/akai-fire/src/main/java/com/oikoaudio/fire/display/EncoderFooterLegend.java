package com.oikoaudio.fire.display;

public final class EncoderFooterLegend {
    public static final String MIXER = "Vol  Pan  S1  S2";

    private static final int[] COLUMN_STARTS = {0, 5, 10, 15};
    private static final int WIDTH = 20;

    private EncoderFooterLegend() {
    }

    public static String of(final String encoder1,
                            final String encoder2,
                            final String encoder3,
                            final String encoder4) {
        final String[] labels = {
                normalize(encoder1),
                normalize(encoder2),
                normalize(encoder3),
                normalize(encoder4)
        };
        final StringBuilder legend = new StringBuilder(" ".repeat(WIDTH));
        for (int slot = 0; slot < labels.length; slot++) {
            final String label = labels[slot];
            final int start = COLUMN_STARTS[slot];
            for (int i = 0; i < label.length() && start + i < WIDTH; i++) {
                legend.setCharAt(start + i, label.charAt(i));
            }
        }
        return legend.toString().stripTrailing();
    }

    public static String fromModeInfo(final String modeInfo) {
        final String[] lines = modeInfo == null ? new String[0] : modeInfo.split("\\n");
        final String[] labels = {"--", "--", "--", "--"};
        for (int i = 0; i < labels.length && i < lines.length; i++) {
            labels[i] = labelFromInfoLine(lines[i]);
        }
        if ("Volume".equals(labels[0]) && "Pan".equals(labels[1])
                && "Send 1".equals(labels[2]) && "Send 2".equals(labels[3])) {
            return MIXER;
        }
        return of(labels[0], labels[1], labels[2], labels[3]);
    }

    public static String remoteControls(final String scopePrefix,
                                        final int firstParameterNumber,
                                        final String... parameterNames) {
        final String prefix = normalizeScopePrefix(scopePrefix);
        return of(remoteLabel(prefix, firstParameterNumber, parameterNames, 0),
                remoteLabel(prefix, firstParameterNumber, parameterNames, 1),
                remoteLabel(prefix, firstParameterNumber, parameterNames, 2),
                remoteLabel(prefix, firstParameterNumber, parameterNames, 3));
    }

    public static String remoteModeInfo(final String scopeLabel,
                                        final String scopePrefix,
                                        final int firstParameterNumber,
                                        final String... parameterNames) {
        final String prefix = normalizeScopePrefix(scopePrefix);
        final StringBuilder info = new StringBuilder(normalizeScopeLabel(scopeLabel));
        for (int slot = 0; slot < 4; slot++) {
            info.append('\n')
                    .append(slot + 1)
                    .append(": ")
                    .append(remoteDetailLabel(prefix, firstParameterNumber, parameterNames, slot));
        }
        return info.toString();
    }

    private static String normalize(final String label) {
        if (label == null || label.isBlank()) {
            return "--";
        }
        return label.length() <= 4 ? label : label.substring(0, 4);
    }

    private static String labelFromInfoLine(final String line) {
        if (line == null || line.isBlank()) {
            return "--";
        }
        final int colon = line.indexOf(':');
        final String withoutIndex = colon >= 0 ? line.substring(colon + 1) : line;
        final String primary = withoutIndex.split("/", 2)[0].trim();
        return primary.isBlank() ? "--" : primary;
    }

    private static String remoteLabel(final String scopePrefix,
                                      final int firstParameterNumber,
                                      final String[] parameterNames,
                                      final int slot) {
        final String name = parameterName(parameterNames, slot);
        return isUsefulRemoteName(name) ? name : scopePrefix + (firstParameterNumber + slot);
    }

    private static String remoteDetailLabel(final String scopePrefix,
                                            final int firstParameterNumber,
                                            final String[] parameterNames,
                                            final int slot) {
        final String name = parameterName(parameterNames, slot);
        return isUsefulRemoteName(name) ? name : scopePrefix + (firstParameterNumber + slot) + " Remote";
    }

    private static String parameterName(final String[] parameterNames, final int slot) {
        if (parameterNames == null || slot >= parameterNames.length) {
            return "";
        }
        return parameterNames[slot];
    }

    private static boolean isUsefulRemoteName(final String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        return !name.trim().matches("(?i)remote( control)?\\s*\\d+");
    }

    private static String normalizeScopePrefix(final String scopePrefix) {
        if (scopePrefix == null || scopePrefix.isBlank()) {
            return "R";
        }
        return scopePrefix.substring(0, 1).toUpperCase();
    }

    private static String normalizeScopeLabel(final String scopeLabel) {
        return scopeLabel == null || scopeLabel.isBlank() ? "Remotes" : scopeLabel;
    }
}
