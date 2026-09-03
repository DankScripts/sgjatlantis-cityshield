package com.mustangdoc.sgjpatch.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client-only development controls. Production gameplay defaults remain disabled. */
public final class SGJPatchClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLE_F8_EDITOR;
    public static final ModConfigSpec SPEC;

    static {
        BUILDER.push("debugger");
        ENABLE_F8_EDITOR = BUILDER
                .comment(
                        "Enables the F8 center-triangle position editor in the Atlantis DHD GUI.",
                        "Intended only for GUI development; leave false for normal gameplay."
                )
                .define("enableF8Editor", false);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    private SGJPatchClientConfig() {}

    public static boolean f8EditorEnabled() {
        return SPEC.isLoaded() && ENABLE_F8_EDITOR.getAsBoolean();
    }
}
