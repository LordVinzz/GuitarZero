package com.example.guitarzero.engine.map;

import com.example.guitarzero.engine.GameplaySession;
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
    public void mapFileSeedKeepsStringPlacementDeterministic() throws IOException {
        MapFile firstMap = createMapFile(42L);
        MapFile secondMap = createMapFile(42L);
        MapFile differentSeedMap = createMapFile(7L);

        List<MapFile.StoredNote> firstNotes = firstMap.getStoredNotes();
        List<MapFile.StoredNote> secondNotes = secondMap.getStoredNotes();
        List<MapFile.StoredNote> differentSeedNotes = differentSeedMap.getStoredNotes();

        assertEquals(firstNotes.size(), secondNotes.size());
        assertEquals(firstNotes.size(), differentSeedNotes.size());

        boolean foundDifferentString = false;
        for (int index = 0; index < firstNotes.size(); index++) {
            MapFile.StoredNote firstNote = firstNotes.get(index);
            MapFile.StoredNote secondNote = secondNotes.get(index);
            MapFile.StoredNote differentSeedNote = differentSeedNotes.get(index);

            assertEquals(firstNote.getAbsoluteTimeMs(), secondNote.getAbsoluteTimeMs());
            assertEquals(firstNote.getDurationMs(), secondNote.getDurationMs());
            assertEquals(firstNote.getStringIndex(), secondNote.getStringIndex());

            if (firstNote.getStringIndex() != differentSeedNote.getStringIndex()) {
                foundDifferentString = true;
            }
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
