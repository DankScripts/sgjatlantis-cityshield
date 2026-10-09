package com.dankscripts.sgjatlantis_cityshield.shield;

import net.minecraft.network.chat.Component;

public enum SurveyMark {
    CENTER,
    NORTH,
    SOUTH,
    EAST,
    WEST,
    TOP,
    BOTTOM,
    BOUNDARY;

    public SurveyMark next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public Component label() {
        return Component.translatable("survey_mark.sgjatlantis_cityshield." + name().toLowerCase());
    }
}
