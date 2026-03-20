package com.example.guitarzero.engine.map;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class SmfMidiParser {
    static final class ParseResult {
        private final List<ParsedMidiNote> notes;
        private final List<ParsedTempoChange> tempoChanges;

        ParseResult(List<ParsedMidiNote> notes, List<ParsedTempoChange> tempoChanges) {
            this.notes = notes;
            this.tempoChanges = tempoChanges;
        }

        List<ParsedMidiNote> getNotes() {
            return notes;
        }

        List<ParsedTempoChange> getTempoChanges() {
            return tempoChanges;
        }
    }

    static final class ParsedMidiNote {
        private final long startTimeMs;
        private final long durationMs;
        private final int pitch;

        ParsedMidiNote(long startTimeMs, long durationMs, int pitch) {
            this.startTimeMs = startTimeMs;
            this.durationMs = durationMs;
            this.pitch = pitch;
        }

        long getStartTimeMs() {
            return startTimeMs;
        }

        long getDurationMs() {
            return durationMs;
        }

        int getPitch() {
            return pitch;
        }
    }

    static final class ParsedTempoChange {
        private final long absoluteTimeMs;
        private final long tick;
        private final int microsecondsPerQuarterNote;

        ParsedTempoChange(long absoluteTimeMs, long tick, int microsecondsPerQuarterNote) {
            this.absoluteTimeMs = absoluteTimeMs;
            this.tick = tick;
            this.microsecondsPerQuarterNote = microsecondsPerQuarterNote;
        }

        long getAbsoluteTimeMs() {
            return absoluteTimeMs;
        }

        long getTick() {
            return tick;
        }

        int getMicrosecondsPerQuarterNote() {
            return microsecondsPerQuarterNote;
        }

        float getBeatsPerMinute() {
            return 60000000f / (float) microsecondsPerQuarterNote;
        }
    }

    private static final int META_EVENT = 0xFF;
    private static final int SYSEX_EVENT = 0xF0;
    private static final int ESCAPED_SYSEX_EVENT = 0xF7;
    private static final int SET_TEMPO_META_TYPE = 0x51;
    private static final int DEFAULT_MICROSECONDS_PER_QUARTER = 500000;

    private SmfMidiParser() {
    }

    static ParseResult parseChart(byte[] midiData, int targetChannelIndex) {
        ByteReader reader = new ByteReader(midiData);
        reader.expectChunkId("MThd");

        int headerLength = reader.readInt();
        int format = reader.readUnsignedShort();
        int trackCount = reader.readUnsignedShort();
        int ticksPerQuarterNote = reader.readUnsignedShort();
        if ((ticksPerQuarterNote & 0x8000) != 0) {
            throw new IllegalArgumentException("SMPTE MIDI timing is not supported.");
        }

        int remainingHeaderBytes = headerLength - 6;
        if (remainingHeaderBytes > 0) {
            reader.skip(remainingHeaderBytes);
        }

        List<TempoChange> tempoChanges = new ArrayList<TempoChange>();
        tempoChanges.add(new TempoChange(0L, DEFAULT_MICROSECONDS_PER_QUARTER));
        List<RawMidiNote> rawNotes = new ArrayList<RawMidiNote>();

        for (int trackIndex = 0; trackIndex < trackCount; trackIndex++) {
            reader.expectChunkId("MTrk");
            int trackLength = reader.readInt();
            int trackEndPosition = reader.getPosition() + trackLength;
            parseTrack(reader, trackEndPosition, targetChannelIndex, tempoChanges, rawNotes);
            reader.setPosition(trackEndPosition);
        }

        if (format != 0 && format != 1) {
            throw new IllegalArgumentException("Unsupported MIDI format: " + format);
        }

        List<TempoChange> normalizedTempoChanges = normalizeTempoChanges(tempoChanges);
        Collections.sort(rawNotes, new Comparator<RawMidiNote>() {
            @Override
            public int compare(RawMidiNote left, RawMidiNote right) {
                int startCompare = Long.compare(left.startTick, right.startTick);
                if (startCompare != 0) {
                    return startCompare;
                }
                return Long.compare(left.endTick, right.endTick);
            }
        });

        List<ParsedMidiNote> parsedNotes = new ArrayList<ParsedMidiNote>(rawNotes.size());
        for (RawMidiNote rawNote : rawNotes) {
            long startTimeMs = ticksToMilliseconds(
                    rawNote.startTick,
                    normalizedTempoChanges,
                    ticksPerQuarterNote
            );
            long endTimeMs = ticksToMilliseconds(
                    rawNote.endTick,
                    normalizedTempoChanges,
                    ticksPerQuarterNote
            );
            parsedNotes.add(new ParsedMidiNote(
                    startTimeMs,
                    Math.max(1L, endTimeMs - startTimeMs),
                    rawNote.pitch
            ));
        }

        List<ParsedTempoChange> parsedTempoChanges =
                new ArrayList<ParsedTempoChange>(normalizedTempoChanges.size());
        for (TempoChange tempoChange : normalizedTempoChanges) {
            parsedTempoChanges.add(new ParsedTempoChange(
                    ticksToMilliseconds(
                            tempoChange.tick,
                            normalizedTempoChanges,
                            ticksPerQuarterNote
                    ),
                    tempoChange.tick,
                    tempoChange.microsecondsPerQuarterNote
            ));
        }

        return new ParseResult(parsedNotes, parsedTempoChanges);
    }

    private static void parseTrack(
            ByteReader reader,
            int trackEndPosition,
            int targetChannelIndex,
            List<TempoChange> tempoChanges,
            List<RawMidiNote> rawNotes
    ) {
        long absoluteTick = 0L;
        int runningStatus = -1;
        Map<Integer, ArrayDeque<Long>> activeNotesByPitch = new HashMap<Integer, ArrayDeque<Long>>();

        while (reader.getPosition() < trackEndPosition) {
            absoluteTick += reader.readVariableLengthValue();

            int statusByte = reader.peekUnsignedByte();
            int status;
            if (statusByte < 0x80) {
                if (runningStatus < 0) {
                    throw new IllegalArgumentException("Invalid running status in MIDI track.");
                }
                status = runningStatus;
            } else {
                status = reader.readUnsignedByte();
                if (status != META_EVENT && status != SYSEX_EVENT && status != ESCAPED_SYSEX_EVENT) {
                    runningStatus = status;
                }
            }

            if (status == META_EVENT) {
                int metaType = reader.readUnsignedByte();
                int length = (int) reader.readVariableLengthValue();
                if (metaType == SET_TEMPO_META_TYPE && length == 3) {
                    int microsecondsPerQuarter = reader.readUnsignedMedium();
                    tempoChanges.add(new TempoChange(absoluteTick, microsecondsPerQuarter));
                } else {
                    reader.skip(length);
                }
                continue;
            }

            if (status == SYSEX_EVENT || status == ESCAPED_SYSEX_EVENT) {
                int length = (int) reader.readVariableLengthValue();
                reader.skip(length);
                runningStatus = -1;
                continue;
            }

            int eventType = status & 0xF0;
            int channelIndex = status & 0x0F;

            switch (eventType) {
                case 0x80:
                case 0x90:
                case 0xA0:
                case 0xB0:
                case 0xE0:
                    int data1 = reader.readUnsignedByte();
                    int data2 = reader.readUnsignedByte();
                    if (channelIndex == targetChannelIndex) {
                        handleChannelEvent(
                                absoluteTick,
                                eventType,
                                data1,
                                data2,
                                activeNotesByPitch,
                                rawNotes
                        );
                    }
                    break;
                case 0xC0:
                case 0xD0:
                    reader.readUnsignedByte();
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported MIDI event: 0x" + Integer.toHexString(status));
            }
        }
    }

    private static void handleChannelEvent(
            long absoluteTick,
            int eventType,
            int pitch,
            int velocity,
            Map<Integer, ArrayDeque<Long>> activeNotesByPitch,
            List<RawMidiNote> rawNotes
    ) {
        if (eventType == 0x90 && velocity > 0) {
            ArrayDeque<Long> activeStarts = activeNotesByPitch.get(pitch);
            if (activeStarts == null) {
                activeStarts = new ArrayDeque<Long>();
                activeNotesByPitch.put(pitch, activeStarts);
            }
            activeStarts.addLast(absoluteTick);
            return;
        }

        if (eventType != 0x80 && !(eventType == 0x90 && velocity == 0)) {
            return;
        }

        ArrayDeque<Long> activeStarts = activeNotesByPitch.get(pitch);
        if (activeStarts == null || activeStarts.isEmpty()) {
            return;
        }

        long startTick = activeStarts.removeFirst();
        rawNotes.add(new RawMidiNote(startTick, Math.max(startTick, absoluteTick), pitch));
    }

    private static List<TempoChange> normalizeTempoChanges(List<TempoChange> tempoChanges) {
        Collections.sort(tempoChanges, new Comparator<TempoChange>() {
            @Override
            public int compare(TempoChange left, TempoChange right) {
                return Long.compare(left.tick, right.tick);
            }
        });

        List<TempoChange> normalizedTempoChanges = new ArrayList<TempoChange>(tempoChanges.size());
        for (TempoChange tempoChange : tempoChanges) {
            if (!normalizedTempoChanges.isEmpty()) {
                TempoChange lastTempoChange =
                        normalizedTempoChanges.get(normalizedTempoChanges.size() - 1);
                if (lastTempoChange.tick == tempoChange.tick) {
                    normalizedTempoChanges.set(
                            normalizedTempoChanges.size() - 1,
                            tempoChange
                    );
                    continue;
                }
            }
            normalizedTempoChanges.add(tempoChange);
        }

        return normalizedTempoChanges;
    }

    private static long ticksToMilliseconds(
            long targetTick,
            List<TempoChange> tempoChanges,
            int ticksPerQuarterNote
    ) {
        long currentTick = 0L;
        int currentTempo = DEFAULT_MICROSECONDS_PER_QUARTER;
        double totalMicroseconds = 0d;

        for (TempoChange tempoChange : tempoChanges) {
            if (tempoChange.tick > targetTick) {
                break;
            }

            if (tempoChange.tick > currentTick) {
                totalMicroseconds += ((tempoChange.tick - currentTick) * (double) currentTempo)
                        / ticksPerQuarterNote;
                currentTick = tempoChange.tick;
            }

            currentTempo = tempoChange.microsecondsPerQuarterNote;
        }

        if (targetTick > currentTick) {
            totalMicroseconds += ((targetTick - currentTick) * (double) currentTempo)
                    / ticksPerQuarterNote;
        }

        return Math.round(totalMicroseconds / 1000d);
    }

    private static final class RawMidiNote {
        private final long startTick;
        private final long endTick;
        private final int pitch;

        private RawMidiNote(long startTick, long endTick, int pitch) {
            this.startTick = startTick;
            this.endTick = endTick;
            this.pitch = pitch;
        }
    }

    private static final class TempoChange {
        private final long tick;
        private final int microsecondsPerQuarterNote;

        private TempoChange(long tick, int microsecondsPerQuarterNote) {
            this.tick = tick;
            this.microsecondsPerQuarterNote = microsecondsPerQuarterNote;
        }
    }

    private static final class ByteReader {
        private final byte[] data;
        private int position;

        private ByteReader(byte[] data) {
            this.data = data;
        }

        private int getPosition() {
            return position;
        }

        private void setPosition(int newPosition) {
            if (newPosition < 0 || newPosition > data.length) {
                throw new IllegalArgumentException("Invalid MIDI cursor position.");
            }
            position = newPosition;
        }

        private void expectChunkId(String chunkId) {
            if (position + 4 > data.length) {
                throw new IllegalArgumentException("Unexpected end of MIDI data.");
            }

            for (int index = 0; index < 4; index++) {
                if ((char) data[position + index] != chunkId.charAt(index)) {
                    throw new IllegalArgumentException("Expected MIDI chunk " + chunkId + ".");
                }
            }
            position += 4;
        }

        private int peekUnsignedByte() {
            if (position >= data.length) {
                throw new IllegalArgumentException("Unexpected end of MIDI data.");
            }
            return data[position] & 0xFF;
        }

        private int readUnsignedByte() {
            int value = peekUnsignedByte();
            position++;
            return value;
        }

        private int readUnsignedShort() {
            return (readUnsignedByte() << 8) | readUnsignedByte();
        }

        private int readUnsignedMedium() {
            return (readUnsignedByte() << 16) | (readUnsignedByte() << 8) | readUnsignedByte();
        }

        private int readInt() {
            return (readUnsignedByte() << 24)
                    | (readUnsignedByte() << 16)
                    | (readUnsignedByte() << 8)
                    | readUnsignedByte();
        }

        private long readVariableLengthValue() {
            long value = 0L;

            while (true) {
                int currentByte = readUnsignedByte();
                value = (value << 7) | (currentByte & 0x7F);
                if ((currentByte & 0x80) == 0) {
                    return value;
                }
            }
        }

        private void skip(int byteCount) {
            setPosition(position + byteCount);
        }
    }
}
