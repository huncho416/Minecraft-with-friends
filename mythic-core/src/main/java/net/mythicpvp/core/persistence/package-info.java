/**
 * SpacetimeDB persistence integration for the {@code mythic-core} services.
 *
 * <p>Why a gateway abstraction instead of calling {@link
 * net.mythicpvp.suite.database.schema.MythicSchema} directly from
 * {@link net.mythicpvp.core.rank.RankService},
 * {@link net.mythicpvp.core.rank.GrantService}, and
 * {@link net.mythicpvp.core.punishment.PunishmentService}:
 *
 * <ol>
 *   <li><b>Test isolation.</b> Existing MockBukkit tests for the three
 *       services don't bring up STDB. The {@link
 *       NoopPersistenceGateway} keeps them green.
 *   <li><b>Boot ordering.</b> STDB connection lives further up the
 *       lifecycle than the service registry. The gateway lets services
 *       construct without a connection and have one wired in later.
 *   <li><b>Cross-server sync.</b> The gateway is also the place where
 *       table subscriptions land — so when another server writes a
 *       grant or punishment, this service's local cache reflects the
 *       update without polling.
 * </ol>
 *
 * <p>Wire-up: {@link net.mythicpvp.core.MythicCorePlugin} constructs a
 * {@link StdbPersistenceGateway} when {@code STDB_URI} is set, otherwise
 * falls back to {@link NoopPersistenceGateway}. Services accept the
 * interface in their constructor.
 */
package net.mythicpvp.core.persistence;
