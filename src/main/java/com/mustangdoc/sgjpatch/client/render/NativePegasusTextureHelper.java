package com.mustangdoc.sgjpatch.client.render;

/** Exact locked Pegasus fallback texture mapping used by the physical table. */
public final class NativePegasusTextureHelper {
    private static final String[] PEGASUS = {
            null, "bydo", "robandus", "sibbron", "once_el", "laylox",
            "earth", "tahnan", "setas", "amiwill", "acjesis", "ca_po",
            "arami", "aaxel", "illume", "ramnon", "avoniv", "lenchan",
            "alura", "ecrumig", "symbol20_fix", "zamilloz", "danami",
            "salma", "hacemill", "hamlinto", "olavii", "poco_re", "abrin",
            "roehi", "sandovi", "gilltin", "dawnre", "elenami", "recktic",
            "aldeni", "zeo", "unknown_1", "unknown_2"
    };

    private NativePegasusTextureHelper() {}

    public static String texturePath(int symbol, boolean outer) {
        if (symbol <= 0 || symbol >= PEGASUS.length)
            return null;
        return "textures/gui/pegasus_clean/" + (outer ? "outer/" : "center/")
                + PEGASUS[symbol] + ".png";
    }
}
