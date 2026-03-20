package com.example.guitarzero.engine.map;

import com.example.guitarzero.engine.Note;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

public class MapFileTest {
    private static final int TEST_STRING_COUNT = 4;
    private static final int TEST_CHANNEL_INDEX = 4;

    @Test
    public void mapFileParsesScomIntoSortedPlayableNotes() throws IOException {
        MapFile mapFile = createMapFile(42L);
        List<MapFile.StoredNote> storedNotes = mapFile.getStoredNotes();

        assertFalse(storedNotes.isEmpty());
        assertFalse(mapFile.getTempoChanges().isEmpty());
        assertTrue(storedNotes.get(0).getAbsoluteTimeMs() > 10000L);

        long lastAbsoluteTimeMs = Long.MIN_VALUE;
        for (MapFile.StoredNote storedNote : storedNotes) {
            assertTrue(storedNote.getDurationMs() > 0L);
            assertTrue(storedNote.getStringIndex() >= 0);
            assertTrue(storedNote.getStringIndex() < TEST_STRING_COUNT);
            assertTrue(storedNote.getAbsoluteTimeMs() >= lastAbsoluteTimeMs);
            lastAbsoluteTimeMs = storedNote.getAbsoluteTimeMs();
        }
    }

    @Test
    public void simultaneousNotesOnSameStringKeepOnlyLongestOne() {
        byte[] midiBytes = new byte[] {
                'M', 'T', 'h', 'd',
                0x00, 0x00, 0x00, 0x06,
                0x00, 0x00,
                0x00, 0x01,
                0x01, (byte) 0xE0,
                'M', 'T', 'r', 'k',
                0x00, 0x00, 0x00, 0x1B,
                0x00, (byte) 0xFF, 0x51, 0x03, 0x07, (byte) 0xA1, 0x20,
                0x00, (byte) 0x94, 0x3C, 0x64,
                0x00, 0x40, 0x64,
                (byte) 0x81, 0x70, (byte) 0x84, 0x3C, 0x00,
                0x78, (byte) 0x84, 0x40, 0x00,
                0x00, (byte) 0xFF, 0x2F, 0x00
        };

        MapFile mapFile = MapFile.fromMidiBytes(
                "simultaneous",
                "simultaneous.mid",
                42L,
                0,
                TEST_CHANNEL_INDEX,
                1,
                0L,
                midiBytes
        );

        List<MapFile.StoredNote> storedNotes = mapFile.getStoredNotes();
        assertEquals(1, storedNotes.size());
        assertEquals(375L, storedNotes.get(0).getDurationMs());
    }

    @Test
    public void mapFileSeedKeepsStringPlacementDeterministic() throws IOException {
        MapFile firstMap = createMapFile(42L);
        MapFile secondMap = createMapFile(42L);
        MapFile differentSeedMap = createMapFile(7L);

        List<MapFile.StoredNote> firstNotes = firstMap.getStoredNotes();
        List<MapFile.StoredNote> secondNotes = secondMap.getStoredNotes();
        List<MapFile.StoredNote> differentSeedNotes = differentSeedMap.getStoredNotes();

        assertEquals(firstNotes.size(), secondNotes.size());

        boolean foundDifferentString = false;
        for (int index = 0; index < firstNotes.size(); index++) {
            MapFile.StoredNote firstNote = firstNotes.get(index);
            MapFile.StoredNote secondNote = secondNotes.get(index);

            assertEquals(firstNote.getAbsoluteTimeMs(), secondNote.getAbsoluteTimeMs());
            assertEquals(firstNote.getDurationMs(), secondNote.getDurationMs());
            assertEquals(firstNote.getStringIndex(), secondNote.getStringIndex());
        }

        int comparableCount = Math.min(firstNotes.size(), differentSeedNotes.size());
        for (int index = 0; index < comparableCount; index++) {
            MapFile.StoredNote firstNote = firstNotes.get(index);
            MapFile.StoredNote differentSeedNote = differentSeedNotes.get(index);

            if (firstNote.getAbsoluteTimeMs() != differentSeedNote.getAbsoluteTimeMs()
                    || firstNote.getDurationMs() != differentSeedNote.getDurationMs()
                    || firstNote.getStringIndex() != differentSeedNote.getStringIndex()) {
                foundDifferentString = true;
                break;
            }
        }

        if (firstNotes.size() != differentSeedNotes.size()) {
            foundDifferentString = true;
        }

        assertTrue(foundDifferentString);
    }

    @Test
    public void createRuntimeNotesReturnsFreshNoteInstances() throws IOException {
        MapFile mapFile = createMapFile(42L);

        List<Note> firstRuntimeNotes = mapFile.createRuntimeNotes();
        List<Note> secondRuntimeNotes = mapFile.createRuntimeNotes();

        assertEquals(firstRuntimeNotes.size(), secondRuntimeNotes.size());
        assertFalse(firstRuntimeNotes.isEmpty());
        assertNotSame(firstRuntimeNotes.get(0), secondRuntimeNotes.get(0));
        assertEquals(firstRuntimeNotes.get(0).absoluteTime, secondRuntimeNotes.get(0).absoluteTime);
        assertEquals(firstRuntimeNotes.get(0).duration, secondRuntimeNotes.get(0).duration);
        assertEquals(firstRuntimeNotes.get(0).corde, secondRuntimeNotes.get(0).corde);
    }

    private MapFile createMapFile(long seed) throws IOException {
        return MapFile.fromMidiBytes(
                "scom",
                "scom.mid",
                seed,
                0,
                TEST_CHANNEL_INDEX,
                TEST_STRING_COUNT,
                0L,
                Files.readAllBytes(resolveMidiPath())
        );
    }

    private Path resolveMidiPath() {
        Path projectRootPath = Paths.get("app/src/main/res/raw/scom.mid");
        if (Files.exists(projectRootPath)) {
            return projectRootPath;
        }

        Path modulePath = Paths.get("src/main/res/raw/scom.mid");
        if (Files.exists(modulePath)) {
            return modulePath;
        }

        throw new IllegalStateException("Unable to locate scom.mid for unit tests.");
    }
}
