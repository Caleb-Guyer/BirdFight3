package com.example.birdgame3;

import javafx.scene.paint.Color;

enum PowerUpType {
    HEALTH(Color.RED, "+40 HP"),
    SPEED(Color.CYAN, "SPEED!"),
    RAGE(Color.ORANGE, "RAGE!"),
    SHRINK(Color.PURPLE, "SHRINK!"),
    NEON(Color.MAGENTA.brighter(), "NEON BOOST!"),
    THERMAL(Color.GOLD.brighter(), "THERMAL RISE!"),
    VINE_GRAPPLE(Color.LIMEGREEN.brighter(), "VINE GRAPPLE!"),
    OVERCHARGE(Color.DEEPSKYBLUE.brighter(), "OVERCHARGE!"),
    TITAN(Color.DARKGOLDENROD, "TITAN FORM!"),
    BROADSIDE(Color.web("#FFB74D"), "BROADSIDE!");

    final Color color;
    final String text;

    PowerUpType(Color color, String text) {
        this.color = color;
        this.text = text;
    }
}
