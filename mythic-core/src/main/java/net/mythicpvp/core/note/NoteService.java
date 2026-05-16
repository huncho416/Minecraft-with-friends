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
        return note;
    }

    @Nullable
    public PlayerNote get(long id) {
        return notes.get(id);
    }

    public boolean delete(long id) {
        return notes.remove(id) != null;
    }

    public int clearFor(@NotNull UUID targetUuid) {
        int removed = 0;
        for (PlayerNote note : new ArrayList<>(notes.values())) {
            if (note.targetUuid().equals(targetUuid)) {
                notes.remove(note.id());
                removed++;
            }
        }
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

    @Nullable
    public PlayerNote findByTitle(@NotNull UUID targetUuid, @NotNull String title) {
        for (PlayerNote note : notes.values()) {
            if (note.targetUuid().equals(targetUuid) && note.title().equalsIgnoreCase(title)) {
                return note;
            }
        }
        return null;
    }
}
