package com.oikoaudio.fire.multiclip;

/** Stable identity used to reject delayed writes after a lane/scene retarget. */
public record MulticlipTargetIdentity(long generation, int childPosition, int scene) {}
