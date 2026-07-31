package com.example.birdgame3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Loads the editable screenplay used by Story Mode: The Still Sky. */
final class StoryDialogueScripts {
    static final String RESOURCE_PATH = "/story/the-still-sky-dialogue.txt";

    private StoryDialogueScripts() {
    }

    static Map<String, String> loadBundled() {
        InputStream stream = StoryDialogueScripts.class.getResourceAsStream(RESOURCE_PATH);
        if (stream == null) {
            throw new IllegalStateException("Missing Still Sky dialogue file: " + RESOURCE_PATH);
        }
        try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return parse(reader);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read Still Sky dialogue file: " + RESOURCE_PATH, e);
        }
    }

    static Map<String, String> parse(Reader source) throws IOException {
        BufferedReader reader = source instanceof BufferedReader buffered
                ? buffered
                : new BufferedReader(source);
        Map<String, StringBuilder> sections = new LinkedHashMap<>();
        String currentId = null;
        int lineNumber = 0;
        String row;
        while ((row = reader.readLine()) != null) {
            lineNumber++;
            String trimmed = row.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                String id = trimmed.substring(1, trimmed.length() - 1).trim();
                if (id.isEmpty()) {
                    throw new IllegalArgumentException("Empty dialogue section at line " + lineNumber);
                }
                if (sections.putIfAbsent(id, new StringBuilder()) != null) {
                    throw new IllegalArgumentException("Duplicate dialogue section [" + id + "]");
                }
                currentId = id;
                continue;
            }
            if (currentId == null) {
                throw new IllegalArgumentException(
                        "Dialogue line appears before a [scene_id] section at line " + lineNumber);
            }
            sections.get(currentId).append(trimmed).append('\n');
        }

        Map<String, String> scripts = new LinkedHashMap<>();
        for (Map.Entry<String, StringBuilder> entry : sections.entrySet()) {
            String script = entry.getValue().toString().strip();
            if (script.isEmpty()) {
                throw new IllegalArgumentException(
                        "Dialogue section [" + entry.getKey() + "] has no spoken lines");
            }
            scripts.put(entry.getKey(), script);
        }
        return Collections.unmodifiableMap(scripts);
    }

    static String require(Map<String, String> scripts, String sceneId) {
        String script = scripts.get(sceneId);
        if (script == null || script.isBlank()) {
            throw new IllegalArgumentException(
                    "Missing dialogue section [" + sceneId + "] in " + RESOURCE_PATH);
        }
        return script;
    }
}
