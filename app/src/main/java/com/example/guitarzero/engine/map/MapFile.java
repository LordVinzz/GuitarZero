package com.example.guitarzero.engine.map;

import android.content.Context;
import android.content.res.Resources;

import com.example.guitarzero.engine.AudioPlayer;
import com.example.guitarzero.engine.Note;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public final class MapFile {
    public static final class TempoChange {
        private final long absoluteTimeMs;
        private final long tick;
        private final int microsecondsPerQuarterNote;
        private final float beatsPerMinute;

        public TempoChange(long absoluteTimeMs, long tick, int microsecondsPerQuarterNote) {
            this.absoluteTimeMs = absoluteTimeMs;
            this.tick = tick;
            this.microsecondsPerQuarterNote = microsecondsPerQuarterNote;
            this.beatsPerMinute = 60000000f / (float) microsecondsPerQuarterNote;
        }

        public long getAbsoluteTimeMs() {
            return absoluteTimeMs;
        }

        public long getTick() {
            return tick;
        }

        public int getMicrosecondsPerQuarterNote() {
            return microsecondsPerQuarterNote;
        }

        public float getBeatsPerMinute() {
            return beatsPerMinute;
        }
    }

    public static final class StoredNote {
        private final long absoluteTimeMs;
        private final long durationMs;
        private final int stringIndex;

        public StoredNote(long absoluteTimeMs, long durationMs, int stringIndex) {
            this.absoluteTimeMs = absoluteTimeMs;
            this.durationMs = durationMs;
            this.stringIndex = stringIndex;
        }

        public long getAbsoluteTimeMs() {
            return absoluteTimeMs;
        }

        public long getDurationMs() {
            return durationMs;
        }

        public int getStringIndex() {
            return stringIndex;
        }
    }

    private final String id;
    private final String displayName;
    private final long seed;
    private final int sourceResId;
    private final int midiChannelIndex;
    private final int backgroundAudioResId;
    private final int hitAudioResId;
    private final AudioPlayer backgroundAudioPlayer;
    private final AudioPlayer hitAudioPlayer;
    private final List<TempoChange> tempoChanges;
    private final List<StoredNote> storedNotes;

    private MapFile(
            String id,
            String displayName,
            long seed,
            int sourceResId,
            int midiChannelIndex,
            int backgroundAudioResId,
            int hitAudioResId,
            AudioPlayer backgroundAudioPlayer,
            AudioPlayer hitAudioPlayer,
            List<TempoChange> tempoChanges,
            List<StoredNote> storedNotes
    ) {
        this.id = id;
        this.displayName = displayName;
        this.seed = seed;
        this.sourceResId = sourceResId;
        this.midiChannelIndex = midiChannelIndex;
        this.backgroundAudioResId = backgroundAudioResId;
        this.hitAudioResId = hitAudioResId;
        this.backgroundAudioPlayer = backgroundAudioPlayer;
        this.hitAudioPlayer = hitAudioPlayer;
        this.tempoChanges = Collections.unmodifiableList(new ArrayList<TempoChange>(tempoChanges));
        this.storedNotes = Collections.unmodifiableList(new ArrayList<StoredNote>(storedNotes));
    }

    public static MapFile load(
            Context context,
            int rawResId,
            String id,
            String displayName,
            long seed,
            int midiChannelIndex,
            int stringCount,
            long timelineOffsetMs,
            int backgroundAudioResId,
            int hitAudioResId
    ) {
        Context applicationContext = context.getApplicationContext();
        Resources resources = applicationContext.getResources();
        InputStream inputStream = resources.openRawResource(rawResId);
        try {
            return fromMidiBytes(
                    id,
                    displayName,
                    seed,
                    rawResId,
                    midiChannelIndex,
                    stringCount,
                    timelineOffsetMs,
                    backgroundAudioResId,
                    hitAudioResId,
                    createAudioPlayer(applicationContext, backgroundAudioResId),
                    createAudioPlayer(applicationContext, hitAudioResId),
                    readAllBytes(inputStream)
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load MIDI map: " + displayName, exception);
        } finally {
            try {
                inputStream.close();
            } catch (IOException ignored) {
            }
        }
    }

    static MapFile fromMidiBytes(
            String id,
            String displayName,
            long seed,
            int sourceResId,
            int midiChannelIndex,
            int stringCount,
            long timelineOffsetMs,
            int backgroundAudioResId,
            int hitAudioResId,
            AudioPlayer backgroundAudioPlayer,
            AudioPlayer hitAudioPlayer,
            byte[] midiBytes
    ) {
        if (stringCount <= 0) {
            throw new IllegalArgumentException("stringCount must be > 0");
        }

        SmfMidiParser.ParseResult parseResult =
                SmfMidiParser.parseChart(midiBytes, midiChannelIndex);
        List<SmfMidiParser.ParsedMidiNote> parsedNotes = parseResult.getNotes();
        if (parsedNotes.isEmpty()) {
            throw new IllegalStateException("No playable MIDI notes found for channel " + midiChannelIndex);
        }

        Collections.sort(parsedNotes, new Comparator<SmfMidiParser.ParsedMidiNote>() {
            @Override
            public int compare(SmfMidiParser.ParsedMidiNote left, SmfMidiParser.ParsedMidiNote right) {
                int startCompare = Long.compare(left.getStartTimeMs(), right.getStartTimeMs());
                if (startCompare != 0) {
                    return startCompare;
                }
                int durationCompare = Long.compare(right.getDurationMs(), left.getDurationMs());
                if (durationCompare != 0) {
                    return durationCompare;
                }
                return Integer.compare(left.getPitch(), right.getPitch());
            }
        });

        Random random = new Random(seed);
        List<TempoChange> tempoChanges = new ArrayList<TempoChange>(parseResult.getTempoChanges().size());
        for (SmfMidiParser.ParsedTempoChange parsedTempoChange : parseResult.getTempoChanges()) {
            tempoChanges.add(new TempoChange(
                    parsedTempoChange.getAbsoluteTimeMs() + timelineOffsetMs,
                    parsedTempoChange.getTick(),
                    parsedTempoChange.getMicrosecondsPerQuarterNote()
            ));
        }

        List<StoredNote> storedNotes = new ArrayList<StoredNote>(parsedNotes.size());

        for (SmfMidiParser.ParsedMidiNote parsedNote : parsedNotes) {
            long absoluteTimeMs = Math.max(0L, parsedNote.getStartTimeMs() + timelineOffsetMs);
            long durationMs = Math.max(1L, parsedNote.getDurationMs());
            int stringIndex = random.nextInt(stringCount);
            storedNotes.add(new StoredNote(absoluteTimeMs, durationMs, stringIndex));
        }
        Collections.sort(storedNotes, new Comparator<StoredNote>() {
            @Override
            public int compare(StoredNote left, StoredNote right) {
                int timeCompare = Long.compare(left.getAbsoluteTimeMs(), right.getAbsoluteTimeMs());
                if (timeCompare != 0) {
                    return timeCompare;
                }

                int stringCompare = Integer.compare(left.getStringIndex(), right.getStringIndex());
                if (stringCompare != 0) {
                    return stringCompare;
                }

                return Long.compare(right.getDurationMs(), left.getDurationMs());
            }
        });
        storedNotes = keepLongestSimultaneousNotesOnSameString(storedNotes);

        return new MapFile(
                id,
                displayName,
                seed,
                sourceResId,
                midiChannelIndex,
                backgroundAudioResId,
                hitAudioResId,
                backgroundAudioPlayer,
                hitAudioPlayer,
                tempoChanges,
                storedNotes
        );
    }

    public List<Note> createRuntimeNotes() {
        List<Note> runtimeNotes = new ArrayList<Note>(storedNotes.size());
        for (StoredNote storedNote : storedNotes) {
            runtimeNotes.add(new Note(
                    storedNote.getStringIndex(),
                    storedNote.getAbsoluteTimeMs(),
                    storedNote.getDurationMs(),
                    0f
            ));
        }
        return runtimeNotes;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public long getSeed() {
        return seed;
    }

    public int getSourceResId() {
        return sourceResId;
    }

    public int getMidiChannelIndex() {
        return midiChannelIndex;
    }

    public int getBackgroundAudioResId() {
        return backgroundAudioResId;
    }

    public int getHitAudioResId() {
        return hitAudioResId;
    }

    public List<TempoChange> getTempoChanges() {
        return tempoChanges;
    }

    public List<StoredNote> getStoredNotes() {
        return storedNotes;
    }

    public float getTempoBpmAtTimeMs(long absoluteTimeMs) {
        if (tempoChanges.isEmpty()) {
            return 120f;
        }

        TempoChange currentTempoChange = tempoChanges.get(0);
        for (int index = 1; index < tempoChanges.size(); index++) {
            TempoChange nextTempoChange = tempoChanges.get(index);
            if (nextTempoChange.getAbsoluteTimeMs() > absoluteTimeMs) {
                break;
            }
            currentTempoChange = nextTempoChange;
        }

        return currentTempoChange.getBeatsPerMinute();
    }

    public void startBackgroundAudio() {
        if (backgroundAudioPlayer != null) {
            backgroundAudioPlayer.play();
        }
    }

    public void stopBackgroundAudio() {
        if (backgroundAudioPlayer != null) {
            backgroundAudioPlayer.stop();
        }
    }

    public void playHitAudio() {
        if (hitAudioPlayer != null) {
            hitAudioPlayer.play();
        }
    }

    public void stopHitAudio() {
        if (hitAudioPlayer != null) {
            hitAudioPlayer.stop();
        }
    }

    public void stopAllAudio() {
        stopBackgroundAudio();
        stopHitAudio();
    }

    public void setHitAudioPitch(float pitch) {
        if (hitAudioPlayer != null) {
            hitAudioPlayer.setPitch(pitch);
        }
    }

    private static byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int bytesRead;

        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }

        return outputStream.toByteArray();
    }

    private static AudioPlayer createAudioPlayer(Context context, int resId) {
        if (resId == 0) {
            return null;
        }

        return new AudioPlayer(context, resId);
    }

    private static List<StoredNote> keepLongestSimultaneousNotesOnSameString(
            List<StoredNote> storedNotes
    ) {
        List<StoredNote> filteredNotes = new ArrayList<StoredNote>(storedNotes.size());

        for (StoredNote storedNote : storedNotes) {
            int lastIndex = filteredNotes.size() - 1;
            if (lastIndex >= 0) {
                StoredNote previousNote = filteredNotes.get(lastIndex);
                if (previousNote.getAbsoluteTimeMs() == storedNote.getAbsoluteTimeMs()
                        && previousNote.getStringIndex() == storedNote.getStringIndex()) {
                    if (storedNote.getDurationMs() > previousNote.getDurationMs()) {
                        filteredNotes.set(lastIndex, storedNote);
                    }
                    continue;
                }
            }

            filteredNotes.add(storedNote);
        }

        return filteredNotes;
    }
}
