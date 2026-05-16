package net.mythicpvp.core.note;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class NoteService {

    private final Map<Long, PlayerNote> notes = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();
    private volatile NoteStore store;

    public void setStore(@NotNull NoteStore store) {
        this.store = store;
        NoteStore.LoadResult loaded = store.load();
        for (PlayerNote note : loaded.notes()) {
            notes.put(note.id(), note);
        }
        sequence.set(Math.max(sequence.get(), loaded.maxId()));
    }

    @NotNull
    public PlayerNote add(@NotNull UUID targetUuid,
                          @NotNull String targetName,
                          @NotNull UUID authorUuid,
                          @NotNull String authorName,
                          @NotNull String title,
                          @NotNull String body,
                          @NotNull String serverId) {
        long id = sequence.incrementAndGet();
        PlayerNote note = new PlayerNote(id, targetUuid, targetName, authorUuid, authorName,
                title, body, serverId, System.currentTimeMillis());
        notes.put(id, note);
        flush();
        return note;
    }

    @Nullable
    public PlayerNote get(long id) {
        return notes.get(id);
    }

    public boolean delete(long id) {
        boolean removed = notes.remove(id) != null;
        if (removed) flush();
        return removed;
    }

    public boolean setActive(long id, boolean active) {
        PlayerNote note = notes.get(id);
        if (note == null || note.active() == active) {
            return false;
        }
        note.setActive(active);
        flush();
        return true;
    }

    public int clearFor(@NotNull UUID targetUuid) {
        int removed = 0;
        for (PlayerNote note : new ArrayList<>(notes.values())) {
            if (note.targetUuid().equals(targetUuid)) {
                notes.remove(note.id());
                removed++;
            }
        }
        if (removed > 0) flush();
        return removed;
    }

    @NotNull
    public List<PlayerNote> notesFor(@NotNull UUID targetUuid) {
        List<PlayerNote> out = new ArrayList<>();
        for (PlayerNote note : notes.values()) {
            if (note.targetUuid().equals(targetUuid)) {
                out.add(note);
            }
        }
        out.sort(Comparator.comparingLong(PlayerNote::createdAt).reversed());
        return out;
    }

    @NotNull
    public List<PlayerNote> all() {
        return new ArrayList<>(notes.values());
    }

    @Nullable
    public PlayerNote findByTitle(@NotNull UUID targetUuid, @NotNull String title) {
        for (PlayerNote note : notes.values()) {
            if (note.targetUuid().equals(targetUuid) && note.title().equalsIgnoreCase(title)) {
                return note;
            }
        }
        return null;
    }

    public void flush() {
        if (store != null) {
            store.save(sequence.get(), new ArrayList<>(notes.values()));
        }
    }
}
