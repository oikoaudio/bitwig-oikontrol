package com.oikoaudio.fire.values;

public enum DawColor {
    RED("Red", 0.850, 0.181, 0.141), //
    ORANGE("Orange", 1.000, 0.341, 0.024), //
    LIGHT_BROWN("Light Brown", 0.850, 0.614, 0.063), //
    YELLOW("Yellow", 0.929, 0.796, 0.100), //
    GREEN("Green", 0.000, 0.616, 0.278), //
    COLD_GREEN("Cold Green", 0.000, 0.612, 0.447), //
    GREEN_BLUE("Green Blue", 0.000, 0.620, 0.529), //
    BLUISH_GREEN("Bluish Green", 0.000, 0.600, 0.784), //
    LIGHT_BLUE("Light Blue", 0.267, 0.784, 1.000), //
    BLUE("Blue", 0.000, 0.600, 0.851), //
    DARK_BLUE("Dark Blue", 0.188, 0.063, 0.620), //
    PURPLE("Purple", 0.533, 0.000, 0.620), //
    LIGHT_PURPLE("Light Purple", 0.733, 0.114, 0.992), //
    PINK("Pink", 0.914, 0.063, 0.533), //
    LIGHT_PINK("Light Pink", 1.000, 0.353, 0.706), //
    BROWN("Brown", 0.392, 0.196, 0.000), //
    DARK_BROWN("Dark Brown", 0.243, 0.118, 0.000), //
    GRAY("Gray", 0.498, 0.498, 0.498), //
    DARK_GRAY("Dark Gray", 0.349, 0.349, 0.349), //
    LIGHT_GRAY("Light Gray", 0.725, 0.725, 0.725), //
    SILVER("Silver", 0.800, 0.800, 0.800), //
    WHITE("White", 0.925, 0.925, 0.925), //
    ROSE("Rose", 0.808, 0.243, 0.384), //
    MOSS_GREEN("Moss Green", 0.360, 0.530, 0.196), //
    PURPLE_BLUE("Purple Blue", 0.341, 0.243, 0.620);

    private final String name;
    private final int lookupIndex;

    DawColor(final String name, final double red, final double green, final double blue) {
        this.name = name;
        lookupIndex = (int) Math.floor(red * 255) << 16 //
                | (int) Math.floor(green * 255) << 8 //
                | (int) Math.floor(blue * 255);
    }

    public String getName() {
        return name;
    }

    public int getLookupIndex() {
        return lookupIndex;
    }
}
