package com.example.birdgame3;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

record MatchHistoryEntry(
        long timestampMillis,
        String mode,
        String detail,
        String map,
        String winner,
        int coinsEarned,
        List<Participant> participants
) {
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    MatchHistoryEntry {
        mode = safe(mode);
        detail = safe(detail);
        map = safe(map);
        winner = safe(winner);
        participants = participants == null ? List.of() : List.copyOf(participants);
    }

    record Participant(
            String slotLabel,
            String birdName,
            int damage,
            int eliminations,
            int falls,
            int score,
            int health,
            boolean winner
    ) {
        Participant {
            slotLabel = safe(slotLabel);
            birdName = safe(birdName);
        }
    }

    String serialize() {
        List<String> encodedParticipants = new ArrayList<>(participants.size());
        for (Participant participant : participants) {
            encodedParticipants.add(encode(participant.slotLabel())
                    + "~" + encode(participant.birdName())
                    + "~" + participant.damage()
                    + "~" + participant.eliminations()
                    + "~" + participant.falls()
                    + "~" + participant.score()
                    + "~" + participant.health()
                    + "~" + (participant.winner() ? "1" : "0"));
        }
        return timestampMillis
                + "|" + encode(mode)
                + "|" + encode(detail)
                + "|" + encode(map)
                + "|" + encode(winner)
                + "|" + coinsEarned
                + "|" + String.join(";", encodedParticipants);
    }

    static MatchHistoryEntry deserialize(String encoded) {
        if (encoded == null || encoded.isBlank()) return null;
        String[] parts = encoded.split("\\|", 7);
        if (parts.length < 6) return null;

        long timestamp = parseLong(parts[0], System.currentTimeMillis());
        String mode = decode(parts[1]);
        String detail = decode(parts[2]);
        String map = decode(parts[3]);
        String winner = decode(parts[4]);
        int coinsEarned = parseInt(parts[5], 0);

        List<Participant> participants = new ArrayList<>();
        if (parts.length >= 7 && !parts[6].isBlank()) {
            String[] rows = parts[6].split(";");
            for (String row : rows) {
                if (row == null || row.isBlank()) continue;
                String[] fields = row.split("~", 8);
                if (fields.length < 8) continue;
                participants.add(new Participant(
                        decode(fields[0]),
                        decode(fields[1]),
                        parseInt(fields[2], 0),
                        parseInt(fields[3], 0),
                        parseInt(fields[4], 0),
                        parseInt(fields[5], 0),
                        parseInt(fields[6], 0),
                        "1".equals(fields[7])
                ));
            }
        }

        return new MatchHistoryEntry(timestamp, mode, detail, map, winner, coinsEarned, participants);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String encode(String value) {
        return ENCODER.encodeToString(safe(value).getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String value) {
        if (value == null || value.isBlank()) return "";
        try {
            return new String(DECODER.decode(value), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ignored) {
            return "";
        }
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static long parseLong(String value, long fallback) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
