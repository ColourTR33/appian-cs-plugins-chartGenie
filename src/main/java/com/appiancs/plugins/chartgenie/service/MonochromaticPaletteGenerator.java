package com.appiancs.plugins.chartgenie.service;

import java.awt.Color;
import java.awt.Paint;
import java.util.logging.Logger;

/**
 * Generates monochromatic colour palettes from a base colour by varying lightness.
 * Used by chart strategies to derive palettes from ChartConfiguration.primaryColor.
 */
public final class MonochromaticPaletteGenerator {

    private static final Logger LOG = Logger.getLogger(MonochromaticPaletteGenerator.class.getName());

    private static final int MINIMUM_SHADES = 6;
    private static final float LIGHTNESS_MIN = 0.20f;
    private static final float LIGHTNESS_MAX = 0.80f;

    private static final Paint[] DEFAULT_GRAYSCALE_PALETTE = {
        new Color(51, 51, 51),      // ~20% lightness
        new Color(85, 85, 85),      // ~33% lightness
        new Color(119, 119, 119),   // ~47% lightness
        new Color(153, 153, 153),   // ~60% lightness
        new Color(187, 187, 187),   // ~73% lightness
        new Color(221, 221, 221)    // ~87% lightness
    };

    private MonochromaticPaletteGenerator() {
        // Utility class — not instantiable
    }

    /**
     * Generates a monochromatic palette from a base colour.
     *
     * @param hexColor 6-char hex string (with or without "#")
     * @param count    number of shades to generate (minimum 6)
     * @return Paint array from darkest to lightest
     */
    public static Paint[] generate(String hexColor, int count) {
        int shadeCount = Math.max(count, MINIMUM_SHADES);

        Color baseColor = parseHexColor(hexColor);
        float[] hsl = rgbToHsl(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue());
        float hue = hsl[0];
        float saturation = hsl[1];

        Paint[] palette = new Paint[shadeCount];
        for (int i = 0; i < shadeCount; i++) {
            float lightness;
            if (shadeCount == 1) {
                lightness = (LIGHTNESS_MIN + LIGHTNESS_MAX) / 2.0f;
            } else {
                lightness = LIGHTNESS_MIN + (LIGHTNESS_MAX - LIGHTNESS_MIN) * i / (shadeCount - 1);
            }
            palette[i] = hslToColor(hue, saturation, lightness);
        }

        return palette;
    }

    /**
     * Returns the default grayscale fallback palette.
     *
     * @return a Paint array with at least 6 grayscale shades
     */
    public static Paint[] getDefaultPalette() {
        return DEFAULT_GRAYSCALE_PALETTE.clone();
    }

    /**
     * Resolves a palette from a primary colour string.
     * Returns the generated monochromatic palette for valid hex input,
     * or the default grayscale palette for null/empty/invalid input.
     *
     * @param primaryColor a hex colour string (with or without "#"), or null
     * @return Paint array of at least 6 elements
     */
    public static Paint[] resolve(String primaryColor) {
        if (primaryColor == null || primaryColor.trim().isEmpty()) {
            LOG.warning("primaryColor is null or empty — using default grayscale palette");
            return getDefaultPalette();
        }

        String hex = normalizeHex(primaryColor.trim());
        if (!isValidHex(hex)) {
            LOG.warning(() -> "primaryColor '" + primaryColor + "' is not a valid hex colour — using default grayscale palette");
            return getDefaultPalette();
        }

        return generate(hex, MINIMUM_SHADES);
    }

    /**
     * Strips a leading "#" if present.
     */
    private static String normalizeHex(String hex) {
        if (hex.startsWith("#")) {
            return hex.substring(1);
        }
        return hex;
    }

    /**
     * Returns true if the string is exactly 6 hex characters.
     */
    private static boolean isValidHex(String hex) {
        if (hex.length() != 6) {
            return false;
        }
        for (int i = 0; i < hex.length(); i++) {
            char c = hex.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Parses a 6-character hex string (without "#") into a Color.
     */
    private static Color parseHexColor(String hex) {
        String normalized = normalizeHex(hex);
        int r = Integer.parseInt(normalized.substring(0, 2), 16);
        int g = Integer.parseInt(normalized.substring(2, 4), 16);
        int b = Integer.parseInt(normalized.substring(4, 6), 16);
        return new Color(r, g, b);
    }

    /**
     * Converts RGB (0-255) to HSL (hue 0-360, saturation 0-1, lightness 0-1).
     */
    private static float[] rgbToHsl(int r, int g, int b) {
        float rf = r / 255.0f;
        float gf = g / 255.0f;
        float bf = b / 255.0f;

        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float delta = max - min;

        float lightness = (max + min) / 2.0f;
        float saturation = 0;
        float hue = 0;

        if (delta != 0) {
            saturation = lightness > 0.5f
                ? delta / (2.0f - max - min)
                : delta / (max + min);

            if (max == rf) {
                hue = ((gf - bf) / delta) % 6;
            } else if (max == gf) {
                hue = (bf - rf) / delta + 2;
            } else {
                hue = (rf - gf) / delta + 4;
            }

            hue *= 60;
            if (hue < 0) {
                hue += 360;
            }
        }

        return new float[]{hue, saturation, lightness};
    }

    /**
     * Converts HSL (hue 0-360, saturation 0-1, lightness 0-1) to a Color.
     */
    private static Color hslToColor(float hue, float saturation, float lightness) {
        float c = (1.0f - Math.abs(2.0f * lightness - 1.0f)) * saturation;
        float x = c * (1.0f - Math.abs((hue / 60.0f) % 2 - 1.0f));
        float m = lightness - c / 2.0f;

        float rf, gf, bf;
        if (hue < 60) {
            rf = c; gf = x; bf = 0;
        } else if (hue < 120) {
            rf = x; gf = c; bf = 0;
        } else if (hue < 180) {
            rf = 0; gf = c; bf = x;
        } else if (hue < 240) {
            rf = 0; gf = x; bf = c;
        } else if (hue < 300) {
            rf = x; gf = 0; bf = c;
        } else {
            rf = c; gf = 0; bf = x;
        }

        int r2 = clamp(Math.round((rf + m) * 255));
        int g2 = clamp(Math.round((gf + m) * 255));
        int b2 = clamp(Math.round((bf + m) * 255));

        return new Color(r2, g2, b2);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
