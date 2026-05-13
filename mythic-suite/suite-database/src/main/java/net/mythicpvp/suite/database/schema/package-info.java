/**
 * Java mirror of the {@code mythic-stdb} SpacetimeDB schema.
 *
 * <p>This package is the canonical view the rest of the suite uses to
 * interact with SpacetimeDB. It contains:
 *
 * <ul>
 *   <li>{@link net.mythicpvp.suite.database.schema.SchemaVersion} — the
 *       schema-version constant that {@code mythic-stdb} expects.
 *   <li>Constants mirroring {@code common.rs} (currency, punishment kind,
 *       server role/status, cosmetic type, table names, reducer names).
 *   <li>{@code dto/} — record types matching every public STDB table,
 *       deserialized by {@link com.google.gson.Gson} from subscription
 *       payloads.
 *   <li>{@link net.mythicpvp.suite.database.schema.MythicSchema} —
 *       a typed reducer client that wraps
 *       {@link net.mythicpvp.suite.database.SpacetimeConnection#callReducer}
 *       with one method per reducer.
 * </ul>
 *
 * <p><b>Drift rule:</b> when you add or rename anything in {@code
 * mythic-cord/stdb/src/}, update this package in the same commit. The
 * boot-time {@link net.mythicpvp.suite.database.schema.SchemaVersion#assertMatches}
 * check will refuse to start a server against an STDB host whose
 * {@code SCHEMA_VERSION} does not match the Java constant.
 */
package net.mythicpvp.suite.database.schema;
