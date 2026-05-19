package net.mythicpvp.suite.compat;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class LegacyColorMapper {

    private static final Map<TextColor, Lab> VANILLA_LABS = buildVanillaLabs();
    private static final Map<Integer, NamedTextColor> PALETTE_OVERRIDES = buildPaletteOverrides();

    private LegacyColorMapper() {}

    @NotNull
    public static NamedTextColor nearestLegacy(@NotNull TextColor color) {
        int rgb = color.value();
        NamedTextColor override = PALETTE_OVERRIDES.get(rgb);
        if (override != null) return override;

        Lab target = labFromRgb(rgb);
        NamedTextColor best = NamedTextColor.WHITE;
        double bestDistance = Double.MAX_VALUE;
        for (Map.Entry<TextColor, Lab> entry : VANILLA_LABS.entrySet()) {
            double distance = target.distanceSquared(entry.getValue());
            if (distance < bestDistance) {
                bestDistance = distance;
                best = (NamedTextColor) entry.getKey();
            }
        }
        return best;
    }

    @NotNull
    private static Map<TextColor, Lab> buildVanillaLabs() {
        Map<TextColor, Lab> map = new LinkedHashMap<>();
        map.put(NamedTextColor.BLACK,        labFromRgb(0x000000));
        map.put(NamedTextColor.DARK_BLUE,    labFromRgb(0x0000AA));
        map.put(NamedTextColor.DARK_GREEN,   labFromRgb(0x00AA00));
        map.put(NamedTextColor.DARK_AQUA,    labFromRgb(0x00AAAA));
        map.put(NamedTextColor.DARK_RED,     labFromRgb(0xAA0000));
        map.put(NamedTextColor.DARK_PURPLE,  labFromRgb(0xAA00AA));
        map.put(NamedTextColor.GOLD,         labFromRgb(0xFFAA00));
        map.put(NamedTextColor.GRAY,         labFromRgb(0xAAAAAA));
        map.put(NamedTextColor.DARK_GRAY,    labFromRgb(0x555555));
        map.put(NamedTextColor.BLUE,         labFromRgb(0x5555FF));
        map.put(NamedTextColor.GREEN,        labFromRgb(0x55FF55));
        map.put(NamedTextColor.AQUA,         labFromRgb(0x55FFFF));
        map.put(NamedTextColor.RED,          labFromRgb(0xFF5555));
        map.put(NamedTextColor.LIGHT_PURPLE, labFromRgb(0xFF55FF));
        map.put(NamedTextColor.YELLOW,       labFromRgb(0xFFFF55));
        map.put(NamedTextColor.WHITE,        labFromRgb(0xFFFFFF));
        return map;
    }

    @NotNull
    private static Map<Integer, NamedTextColor> buildPaletteOverrides() {
        Map<Integer, NamedTextColor> map = new HashMap<>();
        map.put(0xF529BE, NamedTextColor.LIGHT_PURPLE);
        map.put(0xFF00F8, NamedTextColor.LIGHT_PURPLE);
        map.put(0xFD37F0, NamedTextColor.LIGHT_PURPLE);
        map.put(0xF639EA, NamedTextColor.LIGHT_PURPLE);
        map.put(0xDD35C4, NamedTextColor.LIGHT_PURPLE);
        map.put(0xF63DF1, NamedTextColor.LIGHT_PURPLE);
        map.put(0xEA21FF, NamedTextColor.LIGHT_PURPLE);

        map.put(0x9CC3FF, NamedTextColor.AQUA);
        map.put(0x87CEEB, NamedTextColor.AQUA);
        map.put(0xADD8E6, NamedTextColor.AQUA);

        map.put(0xFFEC8A, NamedTextColor.YELLOW);
        map.put(0xFFE082, NamedTextColor.YELLOW);
        map.put(0xFFD700, NamedTextColor.GOLD);
        map.put(0xFFAA00, NamedTextColor.GOLD);
        map.put(0xFFA500, NamedTextColor.GOLD);

        map.put(0x9CFF9C, NamedTextColor.GREEN);
        map.put(0x98FB98, NamedTextColor.GREEN);
        map.put(0x55FF55, NamedTextColor.GREEN);

        map.put(0xFF8A8A, NamedTextColor.RED);
        map.put(0xFFB6C1, NamedTextColor.RED);
        map.put(0xFF1493, NamedTextColor.LIGHT_PURPLE);
        map.put(0xFF69B4, NamedTextColor.LIGHT_PURPLE);

        map.put(0xD2D8E0, NamedTextColor.WHITE);
        map.put(0xFFFFFF, NamedTextColor.WHITE);

        map.put(0x7A8AA0, NamedTextColor.GRAY);
        map.put(0xAAAAAA, NamedTextColor.GRAY);
        map.put(0x808080, NamedTextColor.GRAY);
        map.put(0x555555, NamedTextColor.DARK_GRAY);

        map.put(0x8B0000, NamedTextColor.DARK_RED);
        map.put(0xC62828, NamedTextColor.DARK_RED);
        map.put(0xB71C1C, NamedTextColor.DARK_RED);

        map.put(0x4B0082, NamedTextColor.DARK_PURPLE);
        map.put(0x9400D3, NamedTextColor.DARK_PURPLE);
        map.put(0x800080, NamedTextColor.DARK_PURPLE);

        map.put(0x00ACC1, NamedTextColor.DARK_AQUA);
        map.put(0x00BCD4, NamedTextColor.DARK_AQUA);
        map.put(0x008080, NamedTextColor.DARK_AQUA);

        map.put(0x42A5F5, NamedTextColor.BLUE);
        map.put(0x64B5F6, NamedTextColor.BLUE);
        map.put(0x191970, NamedTextColor.DARK_BLUE);
        map.put(0x0000FF, NamedTextColor.BLUE);

        map.put(0x006400, NamedTextColor.DARK_GREEN);
        map.put(0x008000, NamedTextColor.DARK_GREEN);
        map.put(0x66BB6A, NamedTextColor.GREEN);
        map.put(0x81C784, NamedTextColor.GREEN);
        return map;
    }

    @NotNull
    private static Lab labFromRgb(int rgb) {
        double r = ((rgb >> 16) & 0xFF) / 255.0;
        double g = ((rgb >> 8) & 0xFF) / 255.0;
        double b = (rgb & 0xFF) / 255.0;

        r = pivotInverseGamma(r);
        g = pivotInverseGamma(g);
        b = pivotInverseGamma(b);

        double x = (r * 0.4124564 + g * 0.3575761 + b * 0.1804375) / 0.95047;
        double y = (r * 0.2126729 + g * 0.7151522 + b * 0.0721750) / 1.00000;
        double z = (r * 0.0193339 + g * 0.1191920 + b * 0.9503041) / 1.08883;

        double fx = pivotF(x);
        double fy = pivotF(y);
        double fz = pivotF(z);

        double L = 116.0 * fy - 16.0;
        double aLab = 500.0 * (fx - fy);
        double bLab = 200.0 * (fy - fz);
        return new Lab(L, aLab, bLab);
    }

    private static double pivotInverseGamma(double c) {
        return c > 0.04045 ? Math.pow((c + 0.055) / 1.055, 2.4) : c / 12.92;
    }

    private static double pivotF(double t) {
        double cbrtThreshold = 216.0 / 24389.0;
        if (t > cbrtThreshold) return Math.cbrt(t);
        return (24389.0 / 27.0 * t + 16.0) / 116.0;
    }

    private record Lab(double L, double a, double b) {
        double distanceSquared(@NotNull Lab other) {
            double dL = L - other.L;
            double da = a - other.a;
            double db = b - other.b;
            return dL * dL + da * da + db * db;
        }
    }
}
