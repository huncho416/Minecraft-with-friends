/**
 * Player-visible display surfaces — tablist, scoreboard, nametag — bound
 * to rank state, configurable via YAML.
 *
 * <p>Architecture:
 *
 * <pre>
 *   PlayerJoinEvent ─┐
 *   GrantService    ─┤   DisplayService.apply(player)   ┌─► TabManager.setEntry / apply
 *   RankService      ├─►                                ├─► NametagManager.setNametag
 *                    │   reads CoreRank, GrantService   ├─► BoardManager.create / setLines
 *                    │   active rank, server identity   │
 *                    │   resolves %placeholders%        │
 *   /chat color etc ─┘                                  └─► (suite-packet client sync)
 * </pre>
 *
 * <p>Components:
 * <ul>
 *   <li>{@link net.mythicpvp.core.display.PlaceholderResolver} — pure-fn
 *       template engine for {@code %rank%}, {@code %player%},
 *       {@code %server%}, {@code %online%}, {@code %chat_prefix%},
 *       {@code %tab_prefix%}, {@code %nametag_prefix%}.
 *   <li>{@link net.mythicpvp.core.display.DisplayService} — orchestrator
 *       that reads rank state and pushes templates through the suite
 *       managers.
 *   <li>{@link net.mythicpvp.core.display.PlayerSessionListener} —
 *       Bukkit listener for join/quit so display state lifecycle tracks
 *       presence.
 * </ul>
 *
 * <p>Designed so {@link net.mythicpvp.core.rank.RankService} and
 * {@link net.mythicpvp.core.rank.GrantService} call into the display
 * service via a {@link java.util.function.Consumer Consumer&lt;UUID&gt;}
 * refresher — keeps the display tier optional from the rank tier's
 * perspective so existing service tests don't need it.
 */
package net.mythicpvp.core.display;
