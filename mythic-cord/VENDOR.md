# Vendored upstream â€” Infrarust

This snapshot was extracted from https://github.com/Shadowner/Infrarust by
`mythic-cord/tools/vendor-infrarust.{ps1,sh}`. Don't edit files under
`infrarust/` by hand; instead add MythicCord-specific code in
`mythic-cord/proxy/`, `mythic-cord/plugin-routing/`, or
`mythic-cord/stdb-bridge/` â€” those crates depend on the vendored API
and survive re-baselines untouched.

## Current pin

| Key | Value |
|---|---|
| Ref      | `v2.0.0-alpha.6` |
| Commit   | `5ef97c3c2daeee52d3817e79e5655bf98fd1959c` |
| Date     | 2026-04-12 12:12:20 +0200 |

## Re-baselining

```
.\tools\vendor-infrarust.ps1 -Ref v2.0.0-alpha.7   # or whatever ref
```

After re-vendoring, run:

```
cargo build --workspace --features with-infrarust
```

## License

Infrarust is AGPL-3.0 with a plugin exception. `mythic-cord/LICENSE`
preserves both terms verbatim.
