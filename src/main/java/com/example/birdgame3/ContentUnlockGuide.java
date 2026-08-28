package com.example.birdgame3;

import com.example.birdgame3.BirdGame3.BirdType;
import com.example.birdgame3.BirdGame3.MapType;
import com.example.birdgame3.BirdGame3.MapVariant;

import java.util.Objects;

/**
 * Canonical player-facing directions for earning locked content.
 *
 * <p>Unlock checks live in {@link BirdGame3}; this catalog deliberately only
 * describes those checks. Keeping the copy in one place prevents fighter
 * select, Featherpedia, and the Vault from disagreeing about progression.</p>
 */
final class ContentUnlockGuide {
    private ContentUnlockGuide() {
    }

    static String bird(BirdType type) {
        Objects.requireNonNull(type, "type");
        if (CollectibleUnlock.forBird(type) != null) {
            return switch (type) {
                case GRINCHHAWK, VULTURE, OPIUMBIRD ->
                        "Recruit in The Still Sky or find in Card Packs.";
                default -> "Find this fighter in Card Packs.";
            };
        }
        return switch (type) {
            case BAT -> "Claim the Vine Swinger achievement, recruit in The Still Sky, or find in Card Packs.";
            case HEISENBIRD -> "Claim the Lean God achievement, recruit in The Still Sky, or find in Card Packs.";
            case RAVEN -> "Claim the Boss Breaker achievement, recruit in The Still Sky, or find in Card Packs.";
            case TITMOUSE -> "Clear Classic with Hummingbird, recruit in The Still Sky, or find in Card Packs.";
            case FALCON, PHOENIX, ROADRUNNER, ROOSTER ->
                    "Recruit in The Still Sky or find in Card Packs.";
            default -> "Available from the start.";
        };
    }

    static String birdShort(BirdType type) {
        Objects.requireNonNull(type, "type");
        if (bird(type).startsWith("Available")) return "STARTER";
        return switch (type) {
            case BAT, HEISENBIRD, RAVEN, TITMOUSE -> "CHALLENGE / STORY / PACKS";
            case GRINCHHAWK, VULTURE, OPIUMBIRD, FALCON, PHOENIX, ROADRUNNER, ROOSTER ->
                    "STORY / PACKS";
            default -> "CARD PACKS";
        };
    }

    static String map(MapType map) {
        Objects.requireNonNull(map, "map");
        if (CollectibleUnlock.forMap(map) != null) {
            return "Find this stage in Card Packs.";
        }
        return switch (map) {
            case DESERT -> "Find this stage in Card Packs.";
            case CAVE -> "Claim the Echoes Below achievement or find this stage in Card Packs.";
            case BATTLEFIELD -> "Claim the Story Keeper achievement or find this stage in Card Packs.";
            case DOCK -> "Complete Tempest Run or find this stage in Card Packs.";
            case PRISON -> "Complete The Still Sky mission Blackout Key or find this stage in Card Packs.";
            case BEACON_CROWN ->
                    "Complete Adventure Chapter 9: Sky of All Wings, or finish The Still Sky.";
            case RESONANCE_HALL, SIGNAL_SPIRE, SILENT_AMPHITHEATER ->
                    "Complete Charles's Classic route: No Voice But His Own.";
            case GLASSWIND_CAUSEWAY, WORLDSEAM ->
                    "Complete Razorbill's Classic route: The Line Between Worlds.";
            case MIDNIGHT_WORKSHOP -> "Complete Grinch-Hawk's Classic route: The Longest Night.";
            case CARRION_EXCHANGE -> "Complete Vulture's Classic route: Nothing Goes to Waste.";
            case ONEIRIC_OBSERVATORY -> "Complete Opium Bird's Classic route: The Twelfth Future.";
            case STORMGLASS_REFINERY -> "Complete Heisenbird's Classic route: The Perfect Product.";
            default -> "Available from the start.";
        };
    }

    static String variant(MapVariant variant) {
        Objects.requireNonNull(variant, "variant");
        return switch (variant) {
            case STANDARD -> "Available with its main stage.";
            case CROWN_DUEL, PARLIAMENT_ROOFTOPS -> "Available from the start.";
            case SKYBREAK_SPIRES, PEREGRINE_RUN -> "Complete Classic with Falcon.";
            case ASHFALL_REBIRTH, FROZEN_CALDERA -> "Complete Classic with Phoenix.";
            case CARRION_THRONE, SORTING_FLOOR, RECLAMATION_CORE -> "Complete Classic with Vulture.";
            case ROOFTOP_RELAY -> "Complete Classic with Pigeon.";
            case TEMPEST_SUMMIT -> "Complete Classic with Eagle.";
            case HEARTBLOOM_SANCTUARY -> "Complete Classic with Hummingbird.";
            case HARVEST_TRIBUNAL -> "Complete Classic with Turkey.";
            case DAWNWATCH_BASTION -> "Complete Classic with Rooster.";
            case REDLINE_CANYON -> "Complete Classic with Roadrunner.";
            case LAST_ICE_SHELF -> "Complete Classic with Penguin.";
            case STILLWATER_MARSH -> "Complete Classic with Shoebill.";
            case OBSIDIAN_FOUNDRY -> "Complete Classic with Razorbill.";
            case GIFT_VAULT, BELLKEEPER_VAULT -> "Complete Classic with Grinch-Hawk.";
            case WAKING_CHAMBER -> "Complete Classic with Opium Bird.";
            case EYE_OF_THE_SUPERCELL -> "Complete Classic with Heisenbird.";
            case NULL_ROCK_DUEL, NULL_ROC_ASCENDING, VOID_CROWN -> "Unlock Beacon Crown.";
            case TITAN_DOCK -> "Unlock Broken Harbor.";
        };
    }

    static String skin(String key, BirdType type) {
        Objects.requireNonNull(type, "type");
        if (key == null || key.isBlank()) return "Available from the start.";
        if (CollectibleUnlock.forKey(key) != null) return "Find this cosmetic in Card Packs.";
        return switch (key) {
            case "CITY_PIGEON", "OLD_SPARROW" -> "Available from the start.";
            case "NOIR_PIGEON" -> "Complete Pigeon's Episode or Classic route, or find it in Card Packs.";
            case "BEACON_PIGEON" -> "Complete Adventure Chapter 5: Signal of the Beacon.";
            case "STORM_PIGEON" -> "Claim the Rooftop Legacy achievement reward.";
            case "NULL_ROCK_VULTURE" ->
                    "Complete Adventure Chapter 9: Sky of All Wings, or finish The Still Sky.";
            case "IRONCLAD_PELICAN" -> "Finish Tempest Run or claim the Iron Tempest achievement reward.";
            case "RESONANCE_BAT" -> "Claim the Echo Sovereign achievement reward.";
            case "SKY_KING_EAGLE" ->
                    "Complete Pelican's Episode or Eagle's Classic route, or find it in Card Packs.";
            case "ASHEN_SOVEREIGN_PHOENIX" -> "Complete Ashfall Trial";
            case "FREEMAN_PIGEON" -> "Claim Taunt Lord or find this cosmetic in Card Packs.";
            case "CIRCUIT_TITMOUSE" -> "Claim Neon Addict or find this cosmetic in Card Packs.";
            case "SUNFLARE_HUMMINGBIRD" -> "Claim Thermal Rider or find this cosmetic in Card Packs.";
            case "GLACIER_SHOEBILL" -> "Claim Cliff Diver or find this cosmetic in Card Packs.";
            case "ECLIPSE_MOCKINGBIRD" -> "Claim Lounge Lizard or find this cosmetic in Card Packs.";
            case "UMBRA_BAT" -> "Claim Canopy King or find this cosmetic in Card Packs.";
            case "AURORA_PELICAN" -> "Claim Pelican King or find this cosmetic in Card Packs.";
            case "MINT_PENGUIN" -> "Claim Route Pioneer or find this cosmetic in Card Packs.";
            case "TIDE_VULTURE" -> "Clear Boss Rush EX or find this cosmetic in Card Packs.";
            case "SUNFORGE_ROOSTER" -> "Claim Classic Virtuoso or find this cosmetic in Card Packs.";
            case "NOVA_PHOENIX" -> "Claim Phoenix Pilgrimage or find this cosmetic in Card Packs.";
            default -> key.startsWith("CLASSIC_SKIN_")
                    ? "Complete Classic with " + type.name + " or find this cosmetic in Card Packs."
                    : "Find this cosmetic in Card Packs.";
        };
    }
}
