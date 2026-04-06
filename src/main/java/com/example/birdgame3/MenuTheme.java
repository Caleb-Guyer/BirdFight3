package com.example.birdgame3;

import javafx.scene.paint.Color;

final class MenuTheme {
    private static final Color DEFAULT_ACCENT = Color.web("#90A4AE");
    private static final String ROOT_BACKGROUND = "-fx-background-color: linear-gradient(to bottom, #06070A, #0D1017 34%, #171B22 100%);";
    private static final String TITLE_BANNER = "-fx-background-color: linear-gradient(to bottom, #FFE45C, #F8C528);"
            + "-fx-background-radius: 12; -fx-border-color: black; -fx-border-width: 4; -fx-border-radius: 12;";
    private static final String TOP_STRIP = "-fx-background-color: linear-gradient(to right, #8E0D16 0%, #C51A24 40%, #111317 40%, #111317 100%);"
            + "-fx-background-radius: 24; -fx-border-color: black; -fx-border-width: 4; -fx-border-radius: 24;";

    private MenuTheme() {
    }

    static String pageBackground() {
        return ROOT_BACKGROUND;
    }

    static String titleBannerStyle() {
        return TITLE_BANNER;
    }

    static String topStripStyle() {
        return TOP_STRIP;
    }

    static String buttonStyle(String baseColor, double radius) {
        return buttonStyle(baseColor, radius, null);
    }

    static String buttonStyle(String baseColor, double radius, String textColor) {
        Color base = parse(baseColor, Color.web("#455A64"));
        Color top = blend(base, Color.WHITE, 0.18);
        Color bottom = blend(base, Color.BLACK, 0.26);
        Color border = blend(base, Color.BLACK, 0.56);
        Color highlight = blend(base, Color.WHITE, 0.42);
        String resolvedText = textColor == null || textColor.isBlank() ? readableText(base) : textColor;
        double innerRadius = Math.max(0.0, radius - 4.0);
        return "-fx-background-color: linear-gradient(to bottom, " + rgb(top) + ", " + rgb(bottom) + ");"
                + "-fx-text-fill: " + resolvedText + ";"
                + "-fx-font-family: 'Arial Black';"
                + "-fx-font-weight: bold;"
                + "-fx-background-radius: " + radius + ";"
                + "-fx-border-color: " + rgb(highlight) + ", " + rgb(border) + ";"
                + "-fx-border-width: 1.6, 2.8;"
                + "-fx-border-insets: 0, 2;"
                + "-fx-border-radius: " + radius + ", " + innerRadius + ";"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.42), 14, 0.24, 0, 5);";
    }

    static String panelStyle(String accentColor, double radius) {
        Color accent = parse(accentColor, DEFAULT_ACCENT);
        return "-fx-background-color: linear-gradient(to bottom, rgba(18,22,28,0.96), rgba(6,8,11,0.98));"
                + "-fx-background-radius: " + radius + ";"
                + "-fx-border-color: " + rgba(blend(accent, Color.WHITE, 0.18), 0.9) + ";"
                + "-fx-border-width: 3;"
                + "-fx-border-radius: " + radius + ";"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.28), 18, 0.22, 0, 6);";
    }

    static String insetPanelStyle(String accentColor, double radius) {
        Color accent = parse(accentColor, DEFAULT_ACCENT);
        return "-fx-background-color: rgba(255,255,255,0.035);"
                + "-fx-background-radius: " + radius + ";"
                + "-fx-border-color: " + rgba(blend(accent, Color.WHITE, 0.22), 0.62) + ";"
                + "-fx-border-width: 2;"
                + "-fx-border-radius: " + radius + ";";
    }

    static String chipStyle(String accentColor, String borderColor, double radius) {
        Color accent = parse(accentColor, DEFAULT_ACCENT);
        Color border = parse(borderColor, blend(accent, Color.WHITE, 0.42));
        return "-fx-background-color: " + rgba(accent, 0.22) + ";"
                + "-fx-background-radius: " + radius + ";"
                + "-fx-border-color: " + rgba(border, 0.8) + ";"
                + "-fx-border-width: 2;"
                + "-fx-border-radius: " + radius + ";";
    }

    static String listRowStyle(String accentColor, double radius, boolean highlighted) {
        Color accent = parse(accentColor, DEFAULT_ACCENT);
        String background = highlighted ? rgba(blend(accent, Color.BLACK, 0.38), 0.34) : "rgba(255,255,255,0.05)";
        String border = highlighted ? rgba(blend(accent, Color.WHITE, 0.2), 0.88) : "rgba(255,255,255,0.12)";
        return "-fx-background-color: " + background + ";"
                + "-fx-border-color: " + border + ";"
                + "-fx-border-width: 2;"
                + "-fx-background-radius: " + radius + ";"
                + "-fx-border-radius: " + radius + ";";
    }

    private static Color parse(String value, Color fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Color.web(value);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private static Color blend(Color a, Color b, double t) {
        return a.interpolate(b, clamp(t));
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static String readableText(Color base) {
        double luminance = 0.2126 * base.getRed() + 0.7152 * base.getGreen() + 0.0722 * base.getBlue();
        return luminance >= 0.66 ? "#111111" : "white";
    }

    private static String rgb(Color color) {
        int r = (int) Math.round(color.getRed() * 255.0);
        int g = (int) Math.round(color.getGreen() * 255.0);
        int b = (int) Math.round(color.getBlue() * 255.0);
        return String.format("#%02X%02X%02X", r, g, b);
    }

    private static String rgba(Color color, double opacity) {
        int r = (int) Math.round(color.getRed() * 255.0);
        int g = (int) Math.round(color.getGreen() * 255.0);
        int b = (int) Math.round(color.getBlue() * 255.0);
        return String.format("rgba(%d,%d,%d,%.3f)", r, g, b, clamp(opacity));
    }
}
