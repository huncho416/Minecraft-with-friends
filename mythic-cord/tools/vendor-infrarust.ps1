# Vendor Infrarust into mythic-cord/infrarust/ on Windows.
# Mirror of vendor-infrarust.sh — kept in lockstep.
#
# Usage:
#   .\tools\vendor-infrarust.ps1                       # default: v2.0.0-alpha.6
#   .\tools\vendor-infrarust.ps1 -Ref v2.0.0-alpha.7   # specific tag
param(
    [string]$Ref = 'v2.0.0-alpha.6'
)

$ErrorActionPreference = 'Stop'

$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$target = Join-Path $root 'infrarust'
$tmp = New-Item -ItemType Directory -Path ([System.IO.Path]::GetTempPath() + [guid]::NewGuid())

Write-Host "[vendor] target ref: $Ref"

try {
    git clone --depth 1 --branch $Ref `
        https://github.com/Shadowner/Infrarust.git `
        (Join-Path $tmp 'Infrarust') | Out-Null

    Push-Location (Join-Path $tmp 'Infrarust')
    $sha = (git rev-parse HEAD).Trim()
    $date = (git log -1 --format=%ci HEAD).Trim()
    Pop-Location

    if (Test-Path $target) {
        Write-Host "[vendor] removing existing $target"
        Remove-Item -Recurse -Force $target
    }
    New-Item -ItemType Directory -Path $target | Out-Null

    # Mirror with robocopy, excluding .git/.github.
    $src = Join-Path $tmp 'Infrarust'
    robocopy $src $target /MIR /XD .git .github | Out-Null
    if ($LASTEXITCODE -ge 8) {
        throw "robocopy failed with exit code $LASTEXITCODE"
    }

    $vendorMd = @"
# Vendored upstream — Infrarust

This snapshot was extracted from https://github.com/Shadowner/Infrarust by
``mythic-cord/tools/vendor-infrarust.{ps1,sh}``. Don't edit files under
``infrarust/`` by hand; instead add MythicCord-specific code in
``mythic-cord/proxy/``, ``mythic-cord/plugin-routing/``, or
``mythic-cord/stdb-bridge/`` — those crates depend on the vendored API
and survive re-baselines untouched.

## Current pin

| Key | Value |
|---|---|
| Ref      | ``$Ref`` |
| Commit   | ``$sha`` |
| Date     | $date |

## Re-baselining

``````
.\tools\vendor-infrarust.ps1 -Ref v2.0.0-alpha.7   # or whatever ref
``````

After re-vendoring, run:

``````
cargo build --workspace --features with-infrarust
``````

## License

Infrarust is AGPL-3.0 with a plugin exception. ``mythic-cord/LICENSE``
preserves both terms verbatim.
"@
    Set-Content -Path (Join-Path $root 'VENDOR.md') -Value $vendorMd -Encoding utf8
    Write-Host "[vendor] wrote $(Join-Path $root 'VENDOR.md')"

    # Un-comment vendor-managed dep blocks in dependent crates.
    function Uncomment-Block($file) {
        $lines = Get-Content $file
        $inBlock = $false
        $out = foreach ($line in $lines) {
            if ($line -match '>>> BEGIN VENDOR-MANAGED BLOCK') { $inBlock = $true; $line; continue }
            if ($line -match '>>> END VENDOR-MANAGED BLOCK')   { $inBlock = $false; $line; continue }
            if ($inBlock -and $line -match '^# ') { $line -replace '^# ', ''; continue }
            $line
        }
        Set-Content -Path $file -Value $out -Encoding utf8
    }
    Uncomment-Block (Join-Path $root 'plugin-routing\Cargo.toml')
    Uncomment-Block (Join-Path $root 'proxy\Cargo.toml')

    # Flip with-infrarust to actually depend on the un-commented entries.
    $prFile = Join-Path $root 'plugin-routing\Cargo.toml'
    (Get-Content $prFile -Raw) -replace 'with-infrarust = \[\]', 'with-infrarust = ["dep:infrarust-api", "dep:infrarust-core"]' | Set-Content $prFile -Encoding utf8

    $pxFile = Join-Path $root 'proxy\Cargo.toml'
    (Get-Content $pxFile -Raw) -replace '("mythiccord-plugin-routing/with-infrarust",)', "`"dep:infrarust`",`r`n    `$1" | Set-Content $pxFile -Encoding utf8

    Write-Host "[vendor] un-commented vendor-managed blocks in plugin-routing/proxy"
    Write-Host "[vendor] done -> $target"
}
finally {
    Remove-Item -Recurse -Force $tmp -ErrorAction SilentlyContinue
}
