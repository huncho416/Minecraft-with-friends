package net.mythicpvp.suite.database;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpacetimeConnectionTest {

    @Test
    void buildsTypedReducerAndSubscriptionMessages() {
        SpacetimeConnection connection = new SpacetimeConnection("http://localhost:3000", "mythic");
        String reducer = connection.reducerMessage("award_points", Map.of("amount", 10), "abc");
        String subscription = connection.subscriptionMessage("players");
        assertTrue(reducer.contains("\"CallReducer\""));
        assertTrue(reducer.contains("\"reducer\":\"award_points\""));
        assertTrue(reducer.contains("\"args\":\"{\\\"amount\\\":10}\""));
        assertTrue(subscription.contains("\"Subscribe\""));
        assertTrue(subscription.contains("\"query_strings\":[\"SELECT * FROM players\"]"));
        assertThrows(IllegalArgumentException.class, () -> connection.subscriptionMessage("players;drop"));
    }

    @Test
    void disconnectedReducerCallsFailFast() {
        SpacetimeConnection connection = new SpacetimeConnection("http://localhost:3000", "mythic");

        CompletionException exception = assertThrows(CompletionException.class, () ->
                connection.callReducer("award_points", Map.of("amount", 10)).join());

        assertTrue(exception.getCause() instanceof IllegalStateException);
    }

    @Test
    void codecRoundTripsJsonPayloads() {
        GsonSpacetimeCodec codec = new GsonSpacetimeCodec();
        byte[] encoded = codec.encode(Map.of("name", "Mythic"));
        AtomicReference<String> value = new AtomicReference<>(new String(encoded, StandardCharsets.UTF_8));
        assertEquals("{\"name\":\"Mythic\"}", value.get());
    }
}
