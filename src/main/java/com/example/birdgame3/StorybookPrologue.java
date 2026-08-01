package com.example.birdgame3;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Immutable, editable storybook content for The Still Sky prologue. */
final class StorybookPrologue {
    static final String ID = "still_sky_prologue";
    static final String RESOURCE = "/story/still-sky-prologue.txt";
    static final String EPILOGUE_ID = "still_sky_epilogue";
    static final String EPILOGUE_RESOURCE = "/story/still-sky-epilogue.txt";

    enum Illustration {
        FIRST_SKY,
        ELEVEN_LANDS,
        FIRST_FIGHT,
        OPEN_WING,
        AGE_OF_WINGS,
        BLACK_MIGRATIONS,
        PERFECT_WEATHER,
        BARGAINS,
        WATCHED_FIGHTS,
        STILLNESS
    }

    record Page(String id, String title, Illustration illustration, List<String> paragraphs) {
        Page {
            id = requireText(id, "page id");
            title = requireText(title, "page title");
            if (illustration == null) throw new IllegalArgumentException("Page illustration is required");
            paragraphs = List.copyOf(paragraphs == null ? List.of() : paragraphs);
            if (paragraphs.isEmpty() || paragraphs.stream().anyMatch(String::isBlank)) {
                throw new IllegalArgumentException("Page " + id + " must contain nonblank prose");
            }
        }

        String prose() {
            return String.join("\n\n", paragraphs);
        }
    }

    final List<Page> pages;
    final String id;
    final String header;
    final String subtitle;

    private StorybookPrologue(String id, String header, String subtitle, List<Page> pages) {
        if (pages.isEmpty()) throw new IllegalArgumentException("The prologue needs at least one page");
        Set<String> ids = new HashSet<>();
        for (Page page : pages) {
            if (!ids.add(page.id())) throw new IllegalArgumentException("Duplicate prologue page: " + page.id());
        }
        this.id = requireText(id, "storybook id");
        this.header = requireText(header, "storybook header");
        this.subtitle = requireText(subtitle, "storybook subtitle");
        this.pages = Collections.unmodifiableList(new ArrayList<>(pages));
    }

    static StorybookPrologue loadBundled() {
        try (InputStream input = StorybookPrologue.class.getResourceAsStream(RESOURCE)) {
            if (input == null) throw new IllegalStateException("Missing prologue resource: " + RESOURCE);
            return parse(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read prologue resource", e);
        }
    }

    static StorybookPrologue loadEpilogue() {
        try (InputStream input = StorybookPrologue.class.getResourceAsStream(EPILOGUE_RESOURCE)) {
            if (input == null) throw new IllegalStateException("Missing epilogue resource: " + EPILOGUE_RESOURCE);
            return parse(new String(input.readAllBytes(), StandardCharsets.UTF_8),
                    EPILOGUE_ID, "EPILOGUE", "Recorded after the Crown fell");
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read epilogue resource", e);
        }
    }

    static StorybookPrologue parse(String source) {
        return parse(source, ID, "PROLOGUE", "Narrated from the records of the Cave Archive");
    }

    private static StorybookPrologue parse(String source, String id, String header, String subtitle) {
        if (source == null) throw new IllegalArgumentException("Prologue source is required");
        List<Page> pages = new ArrayList<>();
        for (String rawSection : source.replace("\r\n", "\n").split("(?m)^---\\s*$")) {
            List<String> lines = rawSection.lines()
                    .map(String::strip)
                    .filter(line -> !line.startsWith("# ") || line.startsWith("## "))
                    .toList();
            int headerIndex = -1;
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).startsWith("## ")) {
                    headerIndex = i;
                    break;
                }
            }
            if (headerIndex < 0) continue;
            String[] pageHeader = lines.get(headerIndex).substring(3).split("\\|", -1);
            if (pageHeader.length != 3) {
                throw new IllegalArgumentException("Invalid prologue page header: " + lines.get(headerIndex));
            }
            List<String> paragraphs = new ArrayList<>();
            StringBuilder paragraph = new StringBuilder();
            for (int i = headerIndex + 1; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.startsWith("#")) continue;
                if (line.isBlank()) {
                    flushParagraph(paragraphs, paragraph);
                } else {
                    if (!paragraph.isEmpty()) paragraph.append(' ');
                    paragraph.append(line);
                }
            }
            flushParagraph(paragraphs, paragraph);
            pages.add(new Page(
                    pageHeader[0].strip(),
                    pageHeader[1].strip(),
                    Illustration.valueOf(pageHeader[2].strip().toUpperCase(Locale.ROOT)),
                    paragraphs));
        }
        return new StorybookPrologue(id, header, subtitle, pages);
    }

    int wordCount() {
        return pages.stream()
                .map(Page::prose)
                .mapToInt(text -> text.isBlank() ? 0 : text.split("\\s+").length)
                .sum();
    }

    private static void flushParagraph(List<String> paragraphs, StringBuilder paragraph) {
        if (!paragraph.isEmpty()) {
            paragraphs.add(paragraph.toString());
            paragraph.setLength(0);
        }
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required");
        return value.strip();
    }
}
