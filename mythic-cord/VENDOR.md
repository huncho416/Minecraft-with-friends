# Vendored upstream — Infrarust

This file is **regenerated** by `tools/vendor-infrarust.{sh,ps1}`. Until
you run that script, `mythic-cord/infrarust/` does not exist and the
workspace builds the **standalone** proxy (no Minecraft accept loop —
registry citizen + admin HTTP only).

## Bootstrap

```sh
# Linux/macOS
./tools/vendor-infrarust.sh

# Windows
.\tools\vendor-infrarust.ps1
```

Default pin: **`v2.0.0-alpha.6`**. After vendoring:

```sh
cargo build --workspace --features with-infrarust
```

## Rationale

We **fork from Infrarust directly** (not from SpacerCord, which is itself
a fork). SpacerCord is consulted as a reference for the SpacetimeDB
driver pattern — see `mythic-cord/stdb-bridge/src/driver.rs` for the
cherry-pick — but its `module_bindings` are replaced wholesale with
bindings against `mythic-stdb`.

## License

Infrarust is AGPL-3.0 with a plugin exception. See `LICENSE`.
