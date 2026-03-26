package com.example.birdgame3;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

class ShopItem {
    final String name;
    final String description;
    final int cost;
    final List<ShopPreview> previews;
    final ShopRarity rarity;
    final boolean bundle;
    final int bundleValue;
    final Supplier<ShopPackResult> purchase;
    final BooleanSupplier owned;
    final BooleanSupplier available;

    ShopItem(String name, String description, int cost, List<ShopPreview> previews,
             ShopRarity rarity, Supplier<ShopPackResult> purchase, BooleanSupplier available) {
        this(name, description, cost, previews, rarity, false, 0, purchase, () -> false, available);
    }

    private ShopItem(String name, String description, int cost, List<ShopPreview> previews,
                     ShopRarity rarity, boolean bundle, int bundleValue, Supplier<ShopPackResult> purchase,
                     BooleanSupplier owned, BooleanSupplier available) {
        this.name = name;
        this.description = description;
        this.cost = cost;
        this.previews = previews;
        this.rarity = rarity;
        this.bundle = bundle;
        this.bundleValue = bundleValue;
        this.purchase = purchase;
        this.owned = owned;
        this.available = available;
    }
}
