package net.mythicpvp.suite.database;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.RecordComponent;

/**
 * Parses an STDB row payload into a DTO record, accepting both the
 * field-named JSON object form (used by the initial-subscription
 * snapshot in some bridge versions) and the positional JSON array form
 * (used by live TransactionUpdate events).
 *
 * <p>Array form is decoded by reading the DTO's {@link RecordComponent}s
 * in declaration order and mapping each to the array element at the
 * same index. Timestamp components stored as {@code [micros]} subarrays
 * are flattened to their first element.
 *
 * <p>Field types supported: {@code String}, {@code int}, {@code long},
 * {@code float}, {@code double}, {@code boolean}, and STDB
 * {@code Timestamp}/{@code Duration} subarrays (mapped to {@code long}).
 */
public final class StdbRowParser {

    private static final Gson GSON = new Gson();

    private StdbRowParser() {
    }

    /**
     * Parse the payload. Returns {@code null} when the payload does not
     * contain enough fields to fill the DTO or when a field cannot be
     * coerced to the expected type — callers should treat {@code null}
     * as "ignore this row" rather than as an error.
     */
    @Nullable
    public static <T> T parse(@NotNull String payload, @NotNull Class<T> dtoType) {
        JsonElement root;
        try {
            root = JsonParser.parseString(payload);
        } catch (RuntimeException e) {
            return null;
        }
        if (root.isJsonObject()) {
            return GSON.fromJson(root, dtoType);
        }
        if (!root.isJsonArray()) {
            return null;
        }
        if (!dtoType.isRecord()) {
            return null;
        }
        return decodeArrayAsRecord(root.getAsJsonArray(), dtoType);
    }

    @Nullable
    private static <T> T decodeArrayAsRecord(@NotNull JsonArray array, @NotNull Class<T> dtoType) {
        RecordComponent[] components = dtoType.getRecordComponents();
        if (components == null || components.length == 0 || array.size() < components.length) {
            return null;
        }
        JsonObject reshaped = new JsonObject();
        for (int i = 0; i < components.length; i++) {
            RecordComponent component = components[i];
            JsonElement element = array.get(i);
            reshaped.add(component.getName(), unwrap(element, component.getType()));
        }
        try {
            return GSON.fromJson(reshaped, dtoType);
        } catch (RuntimeException e) {
            return null;
        }
    }

    @NotNull
    private static JsonElement unwrap(@NotNull JsonElement element, @NotNull Class<?> targetType) {
        if (element.isJsonArray()) {
            JsonArray inner = element.getAsJsonArray();
            if (inner.size() == 1) {
                return inner.get(0);
            }
            if (inner.isEmpty()) {
                return new JsonPrimitive(0);
            }
            return inner.get(0);
        }
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            JsonElement micros = obj.get("__timestamp_micros_since_unix_epoch__");
            if (micros == null) {
                micros = obj.get("__time_duration_micros__");
            }
            if (micros != null) {
                return micros;
            }
        }
        return element;
    }
}
